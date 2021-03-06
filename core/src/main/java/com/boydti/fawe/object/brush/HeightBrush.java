package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.brush.heightmap.ScalableHeightMap;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.Pattern;
import java.io.File;
import java.io.IOException;

public class HeightBrush implements DoubleActionBrush {

    public final ScalableHeightMap heightMap;
    private final int rotation;
    double yscale = 1;
    private final DoubleActionBrushTool tool;

    public HeightBrush(File file, int rotation, double yscale, DoubleActionBrushTool tool, Clipboard clipboard) {
        this.tool = tool;
        this.rotation = (rotation / 90) % 4;
        this.yscale = yscale;
        if (file == null || !file.exists()) {
            // Since I can't be bothered using separate args, we'll get it from the filename
            if (file.getName().equalsIgnoreCase("#clipboard.png") && clipboard != null) {
                heightMap = ScalableHeightMap.fromClipboard(clipboard);
            } else {
                heightMap = new ScalableHeightMap();
            }
        } else {
            try {
                heightMap = ScalableHeightMap.fromPNG(file);
            } catch (IOException e) {
                throw new FaweException(BBC.BRUSH_HEIGHT_INVALID);
            }
        }
    }

    @Override
    public void build(DoubleActionBrushTool.BrushAction action, EditSession editSession, Vector position, Pattern pattern, double sizeDouble) throws MaxChangedBlocksException {
        int size = (int) (action == DoubleActionBrushTool.BrushAction.PRIMARY ? sizeDouble : -sizeDouble);
        Mask mask = tool.getMask();
        if (mask == Masks.alwaysTrue() || mask == Masks.alwaysTrue2D()) {
            mask = null;
        }
        heightMap.setSize(size);
        heightMap.apply(editSession, mask, position, size, rotation, yscale, true);
    }
}
