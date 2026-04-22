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
import { BASE_URL } from '../config.js';
import { bearerHeaders } from '../helpers/auth.js';
import { __VU } from 'k6/execution';

export function orderFlow(data) {
    if (!data || !data.users || data.users.length === 0) return;

    // Выбираем пользователя циклически по номеру VU
    const userIdx = (__VU - 1) % data.users.length;
    const user = data.users[userIdx];
    const headers = bearerHeaders(user.token);

    // ── Шаг 1: просмотр страницы события ─────────────────────────────────────
    const eventRes = http.get(`${BASE_URL}/api/v1/events/${data.eventId}`);
    check(eventRes, { 'event page 200': (r) => r.status === 200 });
    sleep(0.5);

    // ── Шаг 2: создание заказа ────────────────────────────────────────────────
    const orderBody = JSON.stringify({
        admissionItems: [{ ticketTypeId: data.standardTypeId, quantity: 1 }],
    });

    const orderRes = http.post(
        `${BASE_URL}/api/v1/events/${data.eventId}/orders`,
        orderBody,
        { headers }
    );

    const orderOk = check(orderRes, {
        'create order 201': (r) => r.status === 201,
        'order has paymentUrl': (r) => {
            try { return !!JSON.parse(r.body).paymentUrl; } catch { return false; }
        },
    });

    if (!orderOk) {
        sleep(1);
        return;
    }

    const order = JSON.parse(orderRes.body);
    sleep(0.3);

    // ── Шаг 3: подтверждение оплаты (50 % VU — имитирует успешную оплату) ────
    if (__VU % 2 === 0) {
        const confirmRes = http.post(
            `${BASE_URL}/api/v1/orders/${order.id}/confirm-payment`,
            null,
            { headers }
        );
        check(confirmRes, { 'confirm payment 200': (r) => r.status === 200 });
        sleep(0.2);

        // ── Шаг 4: получение заказа ───────────────────────────────────────────
        const getOrderRes = http.get(
            `${BASE_URL}/api/v1/orders/${order.id}`,
            { headers }
        );
        check(getOrderRes, {
            'get order 200': (r) => r.status === 200,
            'order is paid': (r) => {
                try { return JSON.parse(r.body).status === 'PAID'; } catch { return false; }
            },
        });
    }

    sleep(Math.random() * 1 + 0.5); // пауза между заказами 0.5–1.5 с
}
