/**
 * Подготовка тестовых данных через API.
 * Вызывается один раз в setup() главного скрипта.
 * Возвращает объект, который передаётся во все сценарии как параметр data.
 */
import http from 'k6/http';
import { check, fail } from 'k6';
import { BASE_URL } from '../config.js';
import { registerAndLogin, bearerHeaders } from './auth.js';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

function post(url, body, headers) {
    return http.post(url, body !== null ? JSON.stringify(body) : null, { headers: headers || JSON_HEADERS });
}

function assertOk(res, label) {
    if (res.status < 200 || res.status >= 300) {
        fail(`Setup failed [${label}]: HTTP ${res.status} — ${res.body}`);
    }
    return JSON.parse(res.body);
}

export function setup() {
    // ── Администратор ────────────────────────────────────────────────────────
    const adminPhone = '+79990000001';
    const adminToken = registerAndLogin(adminPhone, 'Load Test Admin');
    const adminH = bearerHeaders(adminToken);

    // ── Категория ────────────────────────────────────────────────────────────
    const category = assertOk(
        post(`${BASE_URL}/api/v1/categories`, { name: 'Концерты (load)', slug: `load-concerts-${Date.now()}` }),
        'create category'
    );

    // ── Организация ──────────────────────────────────────────────────────────
    const org = assertOk(
        post(`${BASE_URL}/api/v1/organizations`, { name: 'Load Test Org', description: 'k6 org' }, adminH),
        'create organization'
    );

    // ── Площадка ─────────────────────────────────────────────────────────────
    const venue = assertOk(
        post(
            `${BASE_URL}/api/v1/venues`,
            {
                name: 'Load Test Venue',
                city: 'Москва',
                address: 'ул. Тестовая, 1',
                capacity: 5000,
                organizationId: org.id,
            },
            adminH
        ),
        'create venue'
    );

    // ── Мероприятие ──────────────────────────────────────────────────────────
    const event = assertOk(
        post(
            `${BASE_URL}/api/v1/events`,
            {
                label: 'Load Test Concert',
                description: 'k6 нагрузочный тест',
                venueId: venue.id,
                categoryId: category.id,
                time: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
                hasSeatMap: false,
            },
            adminH
        ),
        'create event'
    );

    // ── Инвентарь (general admission) ────────────────────────────────────────
    // POST /api/v1/events/{eventId}/inventory/general-admission
    const inventory = assertOk(
        post(
            `${BASE_URL}/api/v1/events/${event.id}/inventory/general-admission`,
            {
                ticketTypes: [
                    { label: 'Стандарт', price: 1000, quota: 10000 },
                    { label: 'VIP',      price: 5000, quota: 1000  },
                ],
            },
            adminH
        ),
        'generate inventory'
    );

    // Получаем ticket-types, чтобы взять ticketTypeId для заказов
    // GET /api/v1/inventory-plans/{eventId}/ticket-types
    const typesRes = http.get(`${BASE_URL}/api/v1/inventory-plans/${event.id}/ticket-types`);
    check(typesRes, { 'ticket-types 200': (r) => r.status === 200 });
    const ticketTypes = JSON.parse(typesRes.body);
    // Берём первый тип (Стандарт)
    const standardTypeId = ticketTypes[0].id;

    // ── Пул пользователей для сценариев ──────────────────────────────────────
    // Создаём 20 пользователей заранее, чтобы не перегружать /auth/register в основной фазе
    const users = [];
    for (let i = 2; i <= 21; i++) {
        const phone = `+7999000${String(i).padStart(4, '0')}`;
        const token = registerAndLogin(phone, `Load User ${i}`);
        users.push({ phone, token });
    }

    return {
        eventId: event.id,
        standardTypeId,
        adminToken,
        users,
    };
}
