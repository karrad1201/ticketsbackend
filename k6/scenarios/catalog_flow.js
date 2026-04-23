import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL } from '../config.js';
import { pickEvent } from '../helpers/events.js';

export function catalogFlow(data) {
    const event = pickEvent(data);

    const citiesRes = http.get(`${BASE_URL}/api/v1/geo/cities`);
    check(citiesRes, { 'cities 200': (r) => r.status === 200 });

    const venuesRes = http.get(`${BASE_URL}/api/v1/venues`);
    check(venuesRes, { 'venues 200': (r) => r.status === 200 });

    const plansRes = http.get(`${BASE_URL}/api/v1/inventory-plans`);
    check(plansRes, { 'inventory plans 200': (r) => r.status === 200 });

    const planRes = http.get(`${BASE_URL}/api/v1/inventory-plans/${event.eventId}`);
    check(planRes, { 'inventory plan 200': (r) => r.status === 200 });

    const ticketTypesRes = http.get(`${BASE_URL}/api/v1/inventory-plans/${event.eventId}/ticket-types`);
    check(ticketTypesRes, { 'ticket types 200': (r) => r.status === 200 });

    sleep(Math.random() * 0.4 + 0.1);
}
