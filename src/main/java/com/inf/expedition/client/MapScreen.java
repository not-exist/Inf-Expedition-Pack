package com.inf.expedition.client;

import com.inf.expedition.Expedition;
import com.inf.expedition.map.MapSnapshot;
import com.inf.expedition.map.NodeSnapshot;
import com.inf.expedition.network.MapMoveRequestPayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MapScreen extends Screen {
    private static final int NODE_HALF = 8;
    private static final ResourceLocation MAP_BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            Expedition.MODID, "textures/gui/map/background.png");
    private static final ResourceLocation PLAYER_ICON = ResourceLocation.fromNamespaceAndPath(
            Expedition.MODID, "textures/gui/map/player.png");

    private int hoveredNodeId = -1;

    public MapScreen() {
        super(Component.translatable("gui.inf_expedition.map"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.blit(MAP_BACKGROUND, 0, 0, 0, 0, width, height, 1024, 768);
        graphics.fill(0, 0, width, height, 0x66000000);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFE8D8B0);
        graphics.drawCenteredString(font,
                Component.translatable("gui.inf_expedition.map.act", MapClientData.act()),
                width / 2, 32, 0xFFB8A888);
        MapSnapshot snapshot = MapClientData.snapshot();
        if (snapshot == null) {
            graphics.drawCenteredString(font,
                    Component.translatable("gui.inf_expedition.map.empty"),
                    width / 2, height / 2, 0xFFFFFFFF);
            return;
        }

        Map<Integer, NodeSnapshot> byId = new HashMap<>();
        for (NodeSnapshot node : snapshot.nodes()) {
            byId.put(node.id(), node);
        }
        int maxColumn = snapshot.nodes().stream().mapToInt(NodeSnapshot::column).max().orElse(0);
        int maxFloor = snapshot.nodes().stream().mapToInt(NodeSnapshot::floor).max().orElse(0);

        for (NodeSnapshot node : snapshot.nodes()) {
            int x = nodeX(node, maxColumn);
            int y = nodeY(node, maxFloor);
            for (int target : node.connections()) {
                NodeSnapshot targetNode = byId.get(target);
                if (targetNode != null) {
                    drawLine(graphics, x, y, nodeX(targetNode, maxColumn), nodeY(targetNode, maxFloor),
                            0xFF5A4A3A);
                }
            }
        }

        hoveredNodeId = -1;
        for (NodeSnapshot node : snapshot.nodes()) {
            int x = nodeX(node, maxColumn);
            int y = nodeY(node, maxFloor);
            boolean current = node.id() == MapClientData.currentNodeId();
            boolean reachable = isReachable(node);
            int border = current ? 0xFFFFFFFF : reachable ? 0xFFFFD166 : 0xFF332A22;
            graphics.fill(x - NODE_HALF - 1, y - NODE_HALF - 1,
                    x + NODE_HALF + 1, y + NODE_HALF + 1, border);
            graphics.blit(nodeTexture(node.type()), x - NODE_HALF, y - NODE_HALF,
                    0, 0, NODE_HALF * 2, NODE_HALF * 2, 32, 32);
            if (node.completed()) {
                graphics.drawString(font, "v", x - 3, y - 4, 0xFF102010, false);
            } else if (node.visited()) {
                graphics.fill(x - NODE_HALF, y - NODE_HALF, x + NODE_HALF, y + NODE_HALF,
                        0x66000000);
            }
            if (mouseX >= x - NODE_HALF - 1 && mouseX <= x + NODE_HALF + 1
                    && mouseY >= y - NODE_HALF - 1 && mouseY <= y + NODE_HALF + 1) {
                hoveredNodeId = node.id();
            }
        }

        NodeSnapshot current = MapClientData.node(MapClientData.currentNodeId());
        if (current != null) {
            int px = nodeX(current, maxColumn);
            int py = nodeY(current, maxFloor);
            graphics.blit(PLAYER_ICON, px - NODE_HALF, py - NODE_HALF,
                    0, 0, NODE_HALF * 2, NODE_HALF * 2, 16, 16);
        }
        graphics.drawString(font, Component.translatable("gui.inf_expedition.map.hint"),
                12, height - 20, 0xFFB8A888, false);
        if (hoveredNodeId >= 0) {
            NodeSnapshot node = byId.get(hoveredNodeId);
            if (node != null) {
                graphics.renderTooltip(font,
                        Component.translatable("node.inf_expedition." + node.type()),
                        mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        MapSnapshot snapshot = MapClientData.snapshot();
        if (snapshot == null) {
            return false;
        }
        int maxColumn = snapshot.nodes().stream().mapToInt(NodeSnapshot::column).max().orElse(0);
        int maxFloor = snapshot.nodes().stream().mapToInt(NodeSnapshot::floor).max().orElse(0);
        for (NodeSnapshot node : snapshot.nodes()) {
            int x = nodeX(node, maxColumn);
            int y = nodeY(node, maxFloor);
            if (mouseX >= x - NODE_HALF - 1 && mouseX <= x + NODE_HALF + 1
                    && mouseY >= y - NODE_HALF - 1 && mouseY <= y + NODE_HALF + 1) {
                if (isReachable(node)) {
                    PacketDistributor.sendToServer(new MapMoveRequestPayload(node.id()));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MapKeyMapping.OPEN_MAP.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isReachable(NodeSnapshot node) {
        if (!MapClientData.canMove() || node.visited()) {
            return false;
        }
        NodeSnapshot current = MapClientData.node(MapClientData.currentNodeId());
        return current != null && current.connections().contains(node.id());
    }

    private int nodeX(NodeSnapshot node, int maxColumn) {
        return MapLayout.x(width, node.column(), maxColumn);
    }

    private int nodeY(NodeSnapshot node, int maxFloor) {
        return MapLayout.y(height, node.floor(), maxFloor);
    }

    private static ResourceLocation nodeTexture(String type) {
        return ResourceLocation.fromNamespaceAndPath(
                Expedition.MODID, "textures/gui/map/nodes/" + type + ".png");
    }

    private static int typeColor(String type) {
        return switch (type) {
            case "battle" -> 0xFFB36B;
            case "elite" -> 0xFF5C5C;
            case "boss" -> 0xCC2D2D;
            case "shop" -> 0xFFD98A;
            case "rest" -> 0xFF7FBF7F;
            case "event" -> 0xFF9B7FD1;
            case "forge" -> 0xFFFFA040;
            case "start" -> 0xFF5CA8FF;
            default -> 0xFF888888;
        };
    }

    private static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            return;
        }
        for (int step = 0; step <= steps; step++) {
            int x = x1 + (x2 - x1) * step / steps;
            int y = y1 + (y2 - y1) * step / steps;
            graphics.fill(x, y, x + 2, y + 2, color);
        }
    }
}
