ServerEvents.loaded(event => {
  let server = event.server
  server.runCommandSilent('gamerule doMobSpawning false')
  server.runCommandSilent('gamerule keepInventory true')
  server.runCommandSilent('gamerule mobGriefing false')
  server.runCommandSilent('gamerule doFireTick false')
  server.runCommandSilent('gamerule naturalRegeneration true')
})
