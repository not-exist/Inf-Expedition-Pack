// Shop room: place a chest at the shop marker and sell simple supplies for emeralds.
let SHOP_ITEMS = [
  'inf:sword_blank',
  'inf:pickaxe_blank',
  'inf:axe_blank',
  'inf:scythe_blank',
  'inf:dagger_blank',
  'minecraft:cooked_beef',
  'minecraft:golden_apple'
]

NativeEvents.onEvent(Java.loadClass('com.inf.expedition.event.RoomEnteredEvent'), event => {
  let player = event.getPlayer()
  let room = event.getRoom()
  if (room.type().id() !== 'shop') {
    player.persistentData.remove('inf_expedition_shop')
    return
  }
  let shops = room.markers().shops()
  if (shops.isEmpty()) return
  let pos = shops.get(0)
  let level = player.level
  level.setBlockAndUpdate(pos, Block.getBlock('minecraft:chest').defaultBlockState())
  player.persistentData.putString('inf_expedition_shop',
    pos.x + ',' + pos.y + ',' + pos.z)
})

NativeEvents.onEvent(Java.loadClass('com.inf.expedition.event.RoomCompletedEvent'), event => {
  let players = event.getServer().getPlayerList().getPlayers()
  for (let i = 0; i < players.size(); i++) {
    players.get(i).persistentData.remove('inf_expedition_shop')
  }
})

BlockEvents.rightClicked(event => {
  let player = event.player
  let key = player.persistentData.getString('inf_expedition_shop')
  if (!key) return
  let parts = key.split(',')
  if (event.block.pos.x != parseInt(parts[0]) ||
      event.block.pos.y != parseInt(parts[1]) ||
      event.block.pos.z != parseInt(parts[2])) return
  let price = player.persistentData.getBoolean('inf_expedition_shop_discount') ? 3 : 4
  if (event.item.id === 'minecraft:emerald' && event.item.count >= price) {
    event.item.shrink(price)
    let pick = SHOP_ITEMS[Math.floor(Math.random() * SHOP_ITEMS.length)]
    player.give(pick)
    player.tell(Text.translate('gui.inf_expedition.shop.bought'))
    event.cancel()
  } else {
    player.tell(Text.translate('gui.inf_expedition.shop.price'))
    event.cancel()
  }
})
