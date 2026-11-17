// Clears are per player; each party member gets +1 on a successful run.
NativeEvents.onEvent(Java.loadClass('com.inf.expedition.event.RunEndedEvent'), event => {
  if (!event.isSuccess()) {
    let failed = event.getParty()
    for (let i = 0; i < failed.size(); i++) {
      failed.get(i).tell(Text.translate('gui.inf_expedition.run.failed'))
    }
    return
  }
  let players = event.getParty()
  for (let i = 0; i < players.size(); i++) {
    let player = players.get(i)
    let clears = player.persistentData.getInt('inf_expedition_clears') + 1
    player.persistentData.putInt('inf_expedition_clears', clears)
    Utils.server.runCommandSilent('scoreboard players set ' + player.username +
      ' inf_expedition_clears ' + clears)
    Utils.server.runCommandSilent('puffish_skills points add ' + player.username +
      ' difficulty 1')
    if (clears >= 3) {
      Utils.server.runCommandSilent('advancement grant ' + player.username +
        ' only inf_expedition:clears_3')
    }
    player.tell(Text.translate('gui.inf_expedition.run.complete'))
  }
})
