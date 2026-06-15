export function computeWorkshopTags(workshopsData, items) {
  const opToWorkshops = new Map();
  for (const ws of workshopsData) {
    if (ws.operationIds) {
      for (const opId of ws.operationIds) {
        if (!opToWorkshops.has(opId)) opToWorkshops.set(opId, new Set());
        opToWorkshops.get(opId).add(ws.name);
      }
    }
  }
  const names = new Set();
  for (const item of items) {
    if (item.operations) {
      for (const op of item.operations) {
        const wsNames = opToWorkshops.get(Number(op.id));
        if (wsNames) {
          for (const n of wsNames) names.add(n);
        }
      }
    }
  }
  const result = Array.from(names).map(n => '#' + n).join('   ');
  console.log('[workshopTags]', result);
  return result;
}
