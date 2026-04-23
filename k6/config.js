/** Общая конфигурация нагрузочных тестов */

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const PROFILE = __ENV.PROFILE || 'load';
export const IS_SMOKE = PROFILE === 'smoke';

export const SMOKE_DATA = {
    eventId: __ENV.SMOKE_EVENT_ID || '11111111-1111-1111-1111-111111111111',
    standardTypeId: __ENV.SMOKE_TICKET_TYPE_ID || '22222222-2222-2222-2222-222222222222',
    adminToken: __ENV.SMOKE_ADMIN_TOKEN || 'k6-smoke-admin-token',
    events: [
        { eventId: '11111111-1111-1111-1111-111111111111', standardTypeId: '22222222-2222-2222-2222-222222222222' },
        { eventId: '11111111-1111-1111-1111-111111111112', standardTypeId: '22222222-2222-2222-2222-222222222223' },
        { eventId: '11111111-1111-1111-1111-111111111113', standardTypeId: '22222222-2222-2222-2222-222222222224' },
        { eventId: '11111111-1111-1111-1111-111111111114', standardTypeId: '22222222-2222-2222-2222-222222222225' },
        { eventId: '11111111-1111-1111-1111-111111111115', standardTypeId: '22222222-2222-2222-2222-222222222226' },
        { eventId: '11111111-1111-1111-1111-111111111116', standardTypeId: '22222222-2222-2222-2222-222222222227' },
        { eventId: '11111111-1111-1111-1111-111111111117', standardTypeId: '22222222-2222-2222-2222-222222222228' },
        { eventId: '11111111-1111-1111-1111-111111111118', standardTypeId: '22222222-2222-2222-2222-222222222229' },
    ],
    users: [
        { phone: '+79990000002', token: __ENV.SMOKE_USER_TOKEN || 'k6-smoke-user-token' },
    ],
};

/** Пороговые значения качества (SLO) */
export const THRESHOLDS = {
    // Публичные browse-эндпоинты: p95 < 200 мс
    'http_req_duration{scenario:browse}': ['p(95)<200'],
    // Авторизация: p95 < 300 мс
    'http_req_duration{scenario:auth_flow}': ['p(95)<300'],
    // Создание заказа (включает вызов WireMock): p95 < 800 мс
    'http_req_duration{scenario:order_flow}': ['p(95)<800'],
    'http_req_duration{scenario:catalog_flow}': ['p(95)<200'],
    'http_req_duration{scenario:account_flow}': ['p(95)<300'],
    // Глобальный процент ошибок < 1 %
    http_req_failed: ['rate<0.01'],
};

/**
 * Профили нагрузки.
 * smoke  — быстрая проверка работоспособности (1 VU, 30 с)
 * load   — реалистичная нагрузка (ramp up → steady → ramp down)
 * stress — поиск предела (постепенное увеличение VU до 200)
 */
export const PROFILES = {
    smoke: {
        scenarios: {
            browse:     { executor: 'constant-vus', vus: 1, duration: '30s', tags: { scenario: 'browse' } },
            auth_flow:  { executor: 'constant-vus', vus: 1, duration: '30s', tags: { scenario: 'auth_flow' } },
            order_flow: { executor: 'constant-vus', vus: 1, duration: '30s', tags: { scenario: 'order_flow' } },
        },
    },

    load: {
        scenarios: {
            // Browse — доминирующий трафик (70 %): 50 VU → 100 VU → 50 VU
            browse: {
                executor: 'ramping-vus',
                startVUs: 0,
                stages: [
                    { duration: '1m', target: 50 },
                    { duration: '3m', target: 100 },
                    { duration: '1m', target: 0 },
                ],
                tags: { scenario: 'browse' },
            },
            // Auth — 15 % трафика
            auth_flow: {
                executor: 'ramping-vus',
                startVUs: 0,
                stages: [
                    { duration: '1m', target: 10 },
                    { duration: '3m', target: 20 },
                    { duration: '1m', target: 0 },
                ],
                tags: { scenario: 'auth_flow' },
            },
            // Order — 15 % трафика (ключевая бизнес-операция)
            order_flow: {
                executor: 'ramping-vus',
                startVUs: 0,
                stages: [
                    { duration: '1m', target: 5 },
                    { duration: '3m', target: 15 },
                    { duration: '1m', target: 0 },
                ],
                tags: { scenario: 'order_flow' },
            },
        },
    },

    stress: {
        scenarios: {
            browse: {
                executor: 'ramping-vus',
                startVUs: 0,
                stages: [
                    { duration: '1m', target: 300 },
                    { duration: '3m', target: 600 },
                    { duration: '1m', target: 0 },
                ],
                tags: { scenario: 'browse' },
            },
            auth_flow: {
                executor: 'ramping-vus',
                startVUs: 0,
                stages: [
                    { duration: '1m', target: 20 },
                    { duration: '3m', target: 40 },
                    { duration: '1m', target: 0 },
                ],
                tags: { scenario: 'auth_flow' },
            },
            order_flow: {
                executor: 'ramping-vus',
                startVUs: 0,
                stages: [
                    { duration: '1m', target: 80 },
                    { duration: '3m', target: 160 },
                    { duration: '1m', target: 0 },
                ],
                tags: { scenario: 'order_flow' },
            },
            catalog_flow: {
                executor: 'ramping-vus',
                startVUs: 0,
                stages: [
                    { duration: '1m', target: 120 },
                    { duration: '3m', target: 240 },
                    { duration: '1m', target: 0 },
                ],
                tags: { scenario: 'catalog_flow' },
            },
            account_flow: {
                executor: 'ramping-vus',
                startVUs: 0,
                stages: [
                    { duration: '1m', target: 60 },
                    { duration: '3m', target: 120 },
                    { duration: '1m', target: 0 },
                ],
                tags: { scenario: 'account_flow' },
            },
        },
    },
};
