/**
 * Сценарий «Auth flow» — регистрация и вход по телефону.
 *
 * Нагрузка: ~15 % общего трафика.
 * Целевые SLO: p95 < 300 мс, error rate < 1 %.
 *
 * Каждый VU симулирует нового пользователя:
 *   send-code → register → /auth/me → logout
 * Затем — повторный вход:
 *   send-code → login → /auth/me
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL } from '../config.js';
import { __VU, __ITER } from 'k6/execution';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

/** Генерирует уникальный номер телефона для пары (VU, итерация). */
function uniquePhone() {
    // VU от 1 до 999, ITER от 0 до 999 → уникальная пара в диапазоне до 999 999
    const n = (__VU * 1000 + __ITER) % 9_000_000 + 1_000_000;
    return `+7916${String(n).padStart(7, '0')}`;
}

export function authFlow() {
    const phone = uniquePhone();

    // ── Шаг 1: запрос SMS-кода ────────────────────────────────────────────────
    const sendRes = http.post(
        `${BASE_URL}/auth/send-code`,
        JSON.stringify({ phone }),
        { headers: JSON_HEADERS }
    );
    check(sendRes, { 'send-code 204': (r) => r.status === 204 });

    sleep(0.1);

    // ── Шаг 2: регистрация ───────────────────────────────────────────────────
    const regRes = http.post(
        `${BASE_URL}/auth/register`,
        JSON.stringify({ phone, code: '123456', fullName: `K6 User ${__VU}-${__ITER}` }),
        { headers: JSON_HEADERS }
    );
    const regOk = check(regRes, {
        'register 201': (r) => r.status === 201,
        'register has token': (r) => {
            try { return !!JSON.parse(r.body).token; } catch { return false; }
        },
    });
    if (!regOk) {
        sleep(1);
        return;
    }

    const token = JSON.parse(regRes.body).token;
    const authHeaders = { ...JSON_HEADERS, Authorization: `Bearer ${token}` };

    sleep(0.2);

    // ── Шаг 3: /auth/me ──────────────────────────────────────────────────────
    const meRes = http.get(`${BASE_URL}/auth/me`, { headers: authHeaders });
    check(meRes, {
        'me 200': (r) => r.status === 200,
        'me has phone': (r) => {
            try { return JSON.parse(r.body).phone === phone; } catch { return false; }
        },
    });

    sleep(0.2);

    // ── Шаг 4: logout ────────────────────────────────────────────────────────
    const logoutRes = http.post(`${BASE_URL}/auth/logout`, null, { headers: authHeaders });
    check(logoutRes, { 'logout 204': (r) => r.status === 204 });

    sleep(0.3);

    // ── Шаг 5: повторный вход ────────────────────────────────────────────────
    http.post(`${BASE_URL}/auth/send-code`, JSON.stringify({ phone }), { headers: JSON_HEADERS });
    const loginRes = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ phone, code: '123456' }),
        { headers: JSON_HEADERS }
    );
    check(loginRes, {
        'login 200': (r) => r.status === 200,
        'login has token': (r) => {
            try { return !!JSON.parse(r.body).token; } catch { return false; }
        },
    });

    sleep(Math.random() * 0.5 + 0.2);
}
