/**
 * РџРѕРґРіРѕС‚РѕРІРєР° С‚РµСЃС‚РѕРІС‹С… РґР°РЅРЅС‹С… С‡РµСЂРµР· API.
 * Р’С‹Р·С‹РІР°РµС‚СЃСЏ РѕРґРёРЅ СЂР°Р· РІ setup() РіР»Р°РІРЅРѕРіРѕ СЃРєСЂРёРїС‚Р°.
 * Р’РѕР·РІСЂР°С‰Р°РµС‚ РѕР±СЉРµРєС‚, РєРѕС‚РѕСЂС‹Р№ РїРµСЂРµРґР°С‘С‚СЃСЏ РІРѕ РІСЃРµ СЃС†РµРЅР°СЂРёРё РєР°Рє РїР°СЂР°РјРµС‚СЂ data.
 */
import http from 'k6/http';
import { check, fail } from 'k6';
import { BASE_URL, IS_SMOKE, SMOKE_DATA } from '../config.js';
import { registerAndLogin, bearerHeaders } from './auth.js';

const JSON_HEADERS = { 'Content-Type': 'application/json' };
let defaultPostHeaders = JSON_HEADERS;
const RUN_ID = (__ENV.K6_USER_RUN_ID || String(Date.now()).slice(-6)).replace(/\D/g, '').padStart(6, '0').slice(-6);

function post(url, body, headers) {
    return http.post(url, body !== null ? JSON.stringify(body) : null, { headers: headers || defaultPostHeaders });
}

function assertOk(res, label) {
    if (res.status < 200 || res.status >= 300) {
        fail(`Setup failed [${label}]: HTTP ${res.status} вЂ” ${res.body}`);
    }
    return JSON.parse(res.body);
}

export function setup() {
    if (IS_SMOKE) {
        return {
            ...SMOKE_DATA,
            users: [
                {
                    phone: `+798${RUN_ID}01`,
                    token: registerAndLogin(`+798${RUN_ID}01`, 'K6 Smoke User'),
                },
            ],
        };
    }

    const loadUsers = [];
    for (let i = 2; i <= 21; i++) {
        const phone = `+79${RUN_ID}${String(i).padStart(3, '0')}`;
        const token = registerAndLogin(phone, `Load User ${i}`);
        loadUsers.push({ phone, token });
    }

    return {
        eventId: SMOKE_DATA.eventId,
        standardTypeId: SMOKE_DATA.standardTypeId,
        adminToken: SMOKE_DATA.adminToken,
        events: SMOKE_DATA.events,
        users: loadUsers,
    };

    // в”Ђв”Ђ РђРґРјРёРЅРёСЃС‚СЂР°С‚РѕСЂ в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    const adminPhone = '+79990000001';
    const adminToken = registerAndLogin(adminPhone, 'Load Test Admin');
    const adminH = bearerHeaders(adminToken);
    defaultPostHeaders = adminH;

    // в”Ђв”Ђ РљР°С‚РµРіРѕСЂРёСЏ в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    const category = assertOk(
        http.get(`${BASE_URL}/api/v1/categories/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa`),
        'get loadtest category'
    );

    // в”Ђв”Ђ РћСЂРіР°РЅРёР·Р°С†РёСЏ в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    const org = assertOk(
        post(
            `${BASE_URL}/api/v1/organizations`,
            { code: `load-test-org-${Date.now()}`, name: 'Load Test Org' },
            adminH
        ),
        'create organization'
    );

    // в”Ђв”Ђ РџР»РѕС‰Р°РґРєР° в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    const venue = assertOk(
        post(
            `${BASE_URL}/api/v1/venues`,
            {
                label: 'Load Test Venue',
                city: {
                    label: 'Москва',
                    subject: { label: 'Москва' },
                },
                address: 'Test street, 1',
                organizationId: org.id,
                spaces: [],
            },
            adminH
        ),
        'create venue'
    );

    // в”Ђв”Ђ РњРµСЂРѕРїСЂРёСЏС‚РёРµ в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    const event = assertOk(
        post(
            `${BASE_URL}/api/v1/events`,
            {
                label: 'Load Test Concert',
                description: 'k6 РЅР°РіСЂСѓР·РѕС‡РЅС‹Р№ С‚РµСЃС‚',
                venueId: venue.id,
                categoryId: category.id,
                time: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
                hasSeatMap: false,
            },
            adminH
        ),
        'create event'
    );

    // в”Ђв”Ђ РРЅРІРµРЅС‚Р°СЂСЊ (general admission) в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    // POST /api/v1/events/{eventId}/inventory/general-admission
    const inventory = assertOk(
        post(
            `${BASE_URL}/api/v1/events/${event.id}/inventory/general-admission`,
            {
                ticketTypes: [
                    { label: 'РЎС‚Р°РЅРґР°СЂС‚', price: 1000, quota: 10000 },
                    { label: 'VIP',      price: 5000, quota: 1000  },
                ],
            },
            adminH
        ),
        'generate inventory'
    );

    // РџРѕР»СѓС‡Р°РµРј ticket-types, С‡С‚РѕР±С‹ РІР·СЏС‚СЊ ticketTypeId РґР»СЏ Р·Р°РєР°Р·РѕРІ
    // GET /api/v1/inventory-plans/{eventId}/ticket-types
    const typesRes = http.get(`${BASE_URL}/api/v1/inventory-plans/${event.id}/ticket-types`);
    check(typesRes, { 'ticket-types 200': (r) => r.status === 200 });
    const ticketTypes = JSON.parse(typesRes.body);
    // Р‘РµСЂС‘Рј РїРµСЂРІС‹Р№ С‚РёРї (РЎС‚Р°РЅРґР°СЂС‚)
    const standardTypeId = ticketTypes[0].id;

    // в”Ђв”Ђ РџСѓР» РїРѕР»СЊР·РѕРІР°С‚РµР»РµР№ РґР»СЏ СЃС†РµРЅР°СЂРёРµРІ в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    // РЎРѕР·РґР°С‘Рј 20 РїРѕР»СЊР·РѕРІР°С‚РµР»РµР№ Р·Р°СЂР°РЅРµРµ, С‡С‚РѕР±С‹ РЅРµ РїРµСЂРµРіСЂСѓР¶Р°С‚СЊ /auth/register РІ РѕСЃРЅРѕРІРЅРѕР№ С„Р°Р·Рµ
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
