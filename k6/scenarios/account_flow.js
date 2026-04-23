import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL } from '../config.js';
import { bearerHeaders } from '../helpers/auth.js';
import { pickEvent } from '../helpers/events.js';
import exec from 'k6/execution';

export function accountFlow(data) {
    if (!data || !data.users || data.users.length === 0) return;

    const vuId = exec.vu.idInTest || 1;
    const user = data.users[(vuId - 1) % data.users.length];
    if (!user || !user.token) return;

    const headers = bearerHeaders(user.token);
    const event = pickEvent(data);

    const meRes = http.get(`${BASE_URL}/auth/me`, { headers });
    check(meRes, { 'account me 200': (r) => r.status === 200 });

    const ticketsRes = http.get(`${BASE_URL}/api/v1/tickets/me`, { headers });
    check(ticketsRes, { 'my tickets 200': (r) => r.status === 200 });

    const favoritesRes = http.get(`${BASE_URL}/api/v1/favorites?page=0&size=50`, { headers });
    check(favoritesRes, { 'favorites 200': (r) => r.status === 200 });

    if (vuId % 4 === 0) {
        const addRes = http.post(
            `${BASE_URL}/api/v1/favorites`,
            JSON.stringify({ eventId: event.eventId }),
            { headers }
        );
        check(addRes, { 'favorite add ok': (r) => r.status === 201 || r.status === 400 });

        const removeRes = http.del(`${BASE_URL}/api/v1/favorites/${event.eventId}`, null, { headers });
        check(removeRes, { 'favorite remove ok': (r) => r.status === 204 || r.status === 404 });
    }

    sleep(Math.random() * 0.5 + 0.2);
}
