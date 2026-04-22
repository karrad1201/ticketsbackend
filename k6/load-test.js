/**
 * Точка входа для k6 нагрузочных тестов.
 *
 * Запуск:
 *   k6 run k6/load-test.js                          # профиль load (по умолчанию)
 *   k6 run -e PROFILE=smoke k6/load-test.js         # smoke
 *   k6 run -e PROFILE=stress k6/load-test.js        # stress
 *   k6 run -e BASE_URL=http://staging:8080 -e PROFILE=load k6/load-test.js
 *
 * Docker:
 *   docker compose -f docker-compose.yml -f docker-compose.k6.yml run k6
 */
import { setup as prepareData } from './helpers/setup.js';
import { browse } from './scenarios/browse.js';
import { authFlow } from './scenarios/auth_flow.js';
import { orderFlow } from './scenarios/order_flow.js';
import { THRESHOLDS, PROFILES } from './config.js';

const profile = __ENV.PROFILE || 'load';

export const options = {
    scenarios: PROFILES[profile].scenarios,
    thresholds: THRESHOLDS,
    // Детальная статистика по перцентилям
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

// setup() вызывается один раз до начала нагрузочного теста
export { prepareData as setup };

/**
 * default-функция вызывается k6 для каждого VU на каждой итерации.
 * k6 роутит VU по сценариям автоматически.
 *
 * Сценарий определяется через тег `scenario` в options.scenarios;
 * здесь мы вручную вызываем нужную функцию на основе тега текущего исполнения.
 */
import exec from 'k6/execution';

export default function (data) {
    const scenarioName = exec.scenario.name;

    switch (scenarioName) {
        case 'browse':
            browse(data);
            break;
        case 'auth_flow':
            authFlow();
            break;
        case 'order_flow':
            orderFlow(data);
            break;
        default:
            browse(data);
    }
}
