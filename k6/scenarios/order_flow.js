/**
 * Сценарий «Order flow» — ключевая бизнес-операция: создание заказа.
 *
 * Нагрузка: ~15 % общего трафика.
 * Целевые SLO: p95 < 800 мс (включает вызов WireMock-платёжного шлюза), error rate < 1 %.
 *
 * Последовательность действий VU:
 *   1. Берёт токен из пула пользователей (создан в setup)
 *   2. Просматривает страницу события
 *   3. Создаёт заказ (admission билеты)
 *   4. Опционально: подтверждает оплату через mock-callback
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { BASE_URL } from '../config.js';
import { bearerHeaders } from '../helpers/auth.js';
import { pickEvent } from '../helpers/events.js';
import exec from 'k6/execution';

const createOrderStatuses = new Counter('order_create_statuses');
const confirmPaymentStatuses = new Counter('order_confirm_statuses');
const getOrderStatuses = new Counter('order_get_statuses');
const confirmPayment2xx = new Counter('order_confirm_2xx');
const confirmPayment4xx = new Counter('order_confirm_4xx');
const confirmPayment5xx = new Counter('order_confirm_5xx');
const confirmPaymentTimeout = new Counter('order_confirm_timeout');
const debugOrder = __ENV.DEBUG_ORDER === '1';

function addConfirmStatus(res, eventId) {
    confirmPaymentStatuses.add(1, { status: String(res.status), eventId });
    if (res.status === 0) {
        confirmPaymentTimeout.add(1, { eventId });
    } else if (res.status >= 200 && res.status < 300) {
        confirmPayment2xx.add(1, { eventId });
    } else if (res.status >= 400 && res.status < 500) {
        confirmPayment4xx.add(1, { eventId });
    } else if (res.status >= 500) {
        confirmPayment5xx.add(1, { eventId });
    }
}

function logOrderFailure(stage, res, context = {}) {
    if (!debugOrder) return;
    console.warn(JSON.stringify({
        stage,
        status: res.status,
        error: res.error,
        url: res.url,
        requestHeaders: res.request && res.request.headers ? res.request.headers : null,
        body: res.body && res.body.length > 500 ? `${res.body.substring(0, 500)}...` : res.body,
        ...context,
    }));
}

export function orderFlow(data) {
    if (!data || !data.users || data.users.length === 0) return;

    // Выбираем пользователя циклически по номеру VU
    const vuId = exec.vu.idInTest || 1;
    const userIdx = (vuId - 1) % data.users.length;
    const user = data.users[userIdx];
    if (!user || !user.token) {
        return;
    }
    const headers = bearerHeaders(user.token);
    const event = pickEvent(data);

    // ── Шаг 1: просмотр страницы события ─────────────────────────────────────
    const eventRes = http.get(`${BASE_URL}/api/v1/events/${event.eventId}`);
    check(eventRes, { 'event page 200': (r) => r.status === 200 });
    sleep(0.5);

    // ── Шаг 2: создание заказа ────────────────────────────────────────────────
    const orderBody = JSON.stringify({
        admissionItems: [{ ticketTypeId: event.standardTypeId, quantity: 1 }],
    });

    const orderRes = http.post(
        `${BASE_URL}/api/v1/events/${event.eventId}/orders`,
        orderBody,
        { headers }
    );
    createOrderStatuses.add(1, { status: String(orderRes.status), eventId: event.eventId });

    const orderOk = check(orderRes, {
        'create order 201': (r) => r.status === 201,
        'order has paymentUrl': (r) => {
            try { return !!JSON.parse(r.body).paymentUrl; } catch { return false; }
        },
    });

    if (!orderOk) {
        logOrderFailure('create-order', orderRes, { eventId: event.eventId, vuId });
        sleep(1);
        return;
    }

    const order = JSON.parse(orderRes.body);
    sleep(0.3);

    // ── Шаг 3: callback платежного шлюза (50 % VU — имитирует успешную оплату) ────
    if (vuId % 2 === 0) {
        const callbackBody = JSON.stringify({
            paymentReference: order.paymentReference,
            status: 'SUCCEEDED',
            payload: JSON.stringify({ source: 'k6', orderId: order.id }),
        });
        const confirmRes = http.post(
            `${BASE_URL}/api/v1/payments/callbacks/mock`,
            callbackBody,
            {
                headers: { 'Content-Type': 'application/json' },
                tags: { name: 'payment-callback' },
            }
        );
        addConfirmStatus(confirmRes, event.eventId);
        const confirmOk = check(confirmRes, { 'confirm payment 200': (r) => r.status === 200 });
        if (!confirmOk) {
            logOrderFailure('confirm-payment', confirmRes, {
                eventId: event.eventId,
                orderId: order.id,
                vuId,
                paymentReference: order.paymentReference,
            });
            sleep(1);
            return;
        }
        sleep(0.2);

        // ── Шаг 4: получение заказа ───────────────────────────────────────────
        const getOrderRes = http.get(
            `${BASE_URL}/api/v1/orders/${order.id}`,
            { headers: bearerHeaders(user.token) }
        );
        getOrderStatuses.add(1, { status: String(getOrderRes.status), eventId: event.eventId });
        check(getOrderRes, {
            'get order 200': (r) => r.status === 200,
            'order is paid': (r) => {
                try { return JSON.parse(r.body).status === 'PAID'; } catch { return false; }
            },
        });
    }

    sleep(Math.random() * 1 + 0.5); // пауза между заказами 0.5–1.5 с
}
