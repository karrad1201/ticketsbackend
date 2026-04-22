/**
 * Сценарий «Browse» — публичные GET-эндпоинты, имитирует анонимных пользователей
 * и зарегистрированных пользователей, просматривающих ленту событий.
 *
 * Нагрузка: ~70 % общего трафика.
 * Целевые SLO: p95 < 200 мс, error rate < 1 %.
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL } from '../config.js';

const CITIES = ['Москва', 'Санкт-Петербург', 'Екатеринбург', 'Казань', 'Новосибирск'];
const QUERIES = ['концерт', 'спектакль', 'выставка', 'фестиваль', ''];

export function browse(data) {
    const city = CITIES[Math.floor(Math.random() * CITIES.length)];
    const q = QUERIES[Math.floor(Math.random() * QUERIES.length)];

    // 1. Лента Discovery (самый популярный эндпоинт)
    const discoveryRes = http.get(`${BASE_URL}/api/v1/discovery?city=${encodeURIComponent(city)}&size=10`);
    check(discoveryRes, {
        'discovery 200': (r) => r.status === 200,
        'discovery has events': (r) => {
            try { return JSON.parse(r.body).events !== undefined; } catch { return false; }
        },
    });

    sleep(0.3);

    // 2. Список событий с пагинацией
    const page = Math.floor(Math.random() * 5);
    const listRes = http.get(`${BASE_URL}/api/v1/events?page=${page}&size=20`);
    check(listRes, { 'events list 200': (r) => r.status === 200 });

    sleep(0.2);

    // 3. Поиск (если есть поисковый запрос)
    if (q) {
        const searchRes = http.get(
            `${BASE_URL}/api/v1/events/search?q=${encodeURIComponent(q)}&city=${encodeURIComponent(city)}&size=10`
        );
        check(searchRes, { 'search 200': (r) => r.status === 200 });
        sleep(0.2);
    }

    // 4. Детальная страница события (если есть eventId из setup)
    if (data && data.eventId) {
        const detailRes = http.get(`${BASE_URL}/api/v1/events/${data.eventId}`);
        check(detailRes, {
            'event detail 200': (r) => r.status === 200,
            'event detail has id': (r) => {
                try { return JSON.parse(r.body).id === data.eventId; } catch { return false; }
            },
        });
        sleep(0.1);
    }

    // 5. Категории (кэшируются на уровне приложения — должно быть очень быстро)
    const catRes = http.get(`${BASE_URL}/api/v1/categories`);
    check(catRes, { 'categories 200': (r) => r.status === 200 });

    sleep(Math.random() * 1 + 0.5); // пауза между «страницами» 0.5–1.5 с
}
