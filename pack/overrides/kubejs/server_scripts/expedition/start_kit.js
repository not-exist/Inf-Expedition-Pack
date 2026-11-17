// Extra starting supplies based on the player's clear count.
NativeEvents.onEvent(Java.loadClass('com.inf.expedition.event.RunStartedEvent'), event => {
  let players = event.getPlayers()
  for (let i = 0; i < players.size(); i++) {
    let player = players.get(i)
    let clears = player.persistentData.getInt('inf_expedition_clears')
    if (clears >= 1 && clears < 3) player.give('inf:sword_blank')
    if (clears >= 3) player.give('inf:hammer')
    if (clears >= 5) player.give('minecraft:potion[potion_contents={potion:"minecraft:strong_healing"}]')
    if (player.persistentData.getBoolean('inf_expedition_bonus_blank')) {
      player.give('inf:sword_blank')
    }
    if (player.persistentData.getBoolean('inf_expedition_bonus_hammer')) {
      player.give('inf:hammer')
    }
    if (player.persistentData.getBoolean('inf_expedition_bonus_potion')) {
      player.give('minecraft:potion[potion_contents={potion:"minecraft:strong_healing"}]')
    }
  }
})
