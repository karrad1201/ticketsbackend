import exec from 'k6/execution';

export function pickEvent(data, offset = 0) {
    const events = data && data.events && data.events.length > 0
        ? data.events
        : [{ eventId: data.eventId, standardTypeId: data.standardTypeId }];
    const vuId = exec.vu.idInTest || 1;
    const iteration = exec.scenario.iterationInTest || 0;
    return events[(vuId + iteration + offset) % events.length];
}
