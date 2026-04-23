import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../config.js';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

/**
 * Регистрирует пользователя и возвращает Bearer-токен.
 * Используется в setup() для подготовки тестовых данных.
 *
 * SMS-код всегда равен «123456» в конфигурации тестового окружения
 * (SmsCodeService заменён на FakeSmsCodeService, который принимает любой 6-значный код
 * или фиксированный код из application-test.yml).
 */
export function registerAndLogin(phone, fullName) {
    // 1. Отправляем код
    http.post(`${BASE_URL}/auth/send-code`, JSON.stringify({ phone }), { headers: JSON_HEADERS });

    // 2. Регистрируемся
    const regRes = http.post(
        `${BASE_URL}/auth/register`,
        JSON.stringify({ phone, code: '123456', fullName }),
        { headers: JSON_HEADERS, responseCallback: http.expectedStatuses(201, 400, 409) }
    );
    if (regRes.status === 201) {
        return JSON.parse(regRes.body).token;
    }

    // Пользователь уже существует — просто логинимся
    http.post(`${BASE_URL}/auth/send-code`, JSON.stringify({ phone }), { headers: JSON_HEADERS });
    const loginRes = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ phone, code: '123456' }),
        { headers: JSON_HEADERS }
    );
    check(loginRes, { 'login ok': (r) => r.status === 200 });
    return JSON.parse(loginRes.body).token;
}

/** Формирует заголовок авторизации из токена. */
export function bearerHeaders(token) {
    return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

/**
 * Логинится под существующим пользователем; возвращает токен или null при ошибке.
 * Для использования внутри VU-итерации (не в setup).
 */
export function login(phone) {
    http.post(`${BASE_URL}/auth/send-code`, JSON.stringify({ phone }), { headers: JSON_HEADERS });
    const res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ phone, code: '123456' }),
        { headers: JSON_HEADERS }
    );
    if (!check(res, { 'login 200': (r) => r.status === 200 })) return null;
    return JSON.parse(res.body).token;
}
