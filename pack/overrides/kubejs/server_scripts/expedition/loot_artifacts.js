// Artifacts as room rewards. LootJS creates one extra pool per chest; the
// empty entry keeps the drop rate around 1/3 for elite/event and 1/2 for boss.
if (typeof LootJS !== 'undefined') {
  LootJS.lootTables(event => {
    let tables = {
      'inf_expedition:chests/elite': 3,
      'inf_expedition:chests/boss': 2,
      'inf_expedition:chests/event': 3
    }
    Object.keys(tables).forEach(id => {
      let table = event.getLootTable(id)
      if (!table) return
      table.createPool(pool => {
        pool.addEntry(LootEntry.tag('artifacts:artifacts').withWeight(1))
        pool.addEntry(LootEntry.empty().withWeight(tables[id]))
      })
    })
  })
}
