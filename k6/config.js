/** Общая конфигурация нагрузочных тестов */

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

/** Пороговые значения качества (SLO) */
export const THRESHOLDS = {
    // Публичные browse-эндпоинты: p95 < 200 мс
    'http_req_duration{scenario:browse}': ['p(95)<200'],
    // Авторизация: p95 < 300 мс
    'http_req_duration{scenario:auth_flow}': ['p(95)<300'],
    // Создание заказа (включает вызов WireMock): p95 < 800 мс
    'http_req_duration{scenario:order_flow}': ['p(95)<800'],
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
                    { duration: '2m', target: 100 },
                    { duration: '2m', target: 200 },
                    { duration: '2m', target: 300 },
                    { duration: '1m', target: 0 },
                ],
                tags: { scenario: 'browse' },
            },
            order_flow: {
                executor: 'ramping-vus',
                startVUs: 0,
                stages: [
                    { duration: '2m', target: 20 },
                    { duration: '2m', target: 50 },
                    { duration: '2m', target: 80 },
                    { duration: '1m', target: 0 },
                ],
                tags: { scenario: 'order_flow' },
            },
        },
    },
};
