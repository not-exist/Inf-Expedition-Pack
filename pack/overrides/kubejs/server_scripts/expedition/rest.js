// Rest room: place a campfire at each rest marker; one heal per room per player.
NativeEvents.onEvent(Java.loadClass('com.inf.expedition.event.RoomEnteredEvent'), event => {
  let player = event.getPlayer()
  let room = event.getRoom()
  if (room.type().id() !== 'rest') {
    player.persistentData.remove('inf_expedition_rest_room')
    player.persistentData.remove('inf_expedition_rest_positions')
    return
  }
  let rest = room.markers().rests()
  if (rest.isEmpty()) return
  let level = player.level
  let positions = []
  for (let i = 0; i < rest.size(); i++) {
    let pos = rest.get(i)
    level.setBlockAndUpdate(pos, Block.getBlock('minecraft:campfire').defaultBlockState())
    positions.push(pos.x + ',' + pos.y + ',' + pos.z)
  }
  player.persistentData.putString('inf_expedition_rest_room', room.id().toString())
  player.persistentData.putString('inf_expedition_rest_positions', positions.join('|'))
})

NativeEvents.onEvent(Java.loadClass('com.inf.expedition.event.RoomCompletedEvent'), event => {
  let players = event.getServer().getPlayerList().getPlayers()
  for (let i = 0; i < players.size(); i++) {
    let data = players.get(i).persistentData
    data.remove('inf_expedition_rest_room')
    data.remove('inf_expedition_rest_positions')
  }
})

BlockEvents.rightClicked(event => {
  let player = event.player
  let roomKey = player.persistentData.getString('inf_expedition_rest_room')
  if (!roomKey) return
  if (event.block.id !== 'minecraft:campfire') return
  let key = event.block.pos.x + ',' + event.block.pos.y + ',' + event.block.pos.z
  let positions = player.persistentData.getString('inf_expedition_rest_positions').split('|')
  if (positions.indexOf(key) < 0) return
  if (player.persistentData.getString('inf_expedition_rest_used') === roomKey) return
  player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + player.getMaxHealth() * 0.5))
  player.persistentData.putString('inf_expedition_rest_used', roomKey)
  player.tell(Text.translate('gui.inf_expedition.rest.healed'))
  event.cancel()
})
