package com.stonebridge.quotesystem.strategy;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.WorkbookWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import com.stonebridge.quotesystem.entity.dto.QuoteExportDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * 终极圆满版：融合 CellWriteHandler (动态合并) 与 WorkbookWriteHandler (全局绘图)
 * 彻底解决内存刷新失效与图片绘制不执行的问题
 */
public class ShapeImageMergeStrategy implements CellWriteHandler, WorkbookWriteHandler {

    private final List<QuoteExportDTO> dataList;
    private static final int PHOTO_COL = 3; // 图片列固定在第 4 列（索引 3）
    private final int[] absRowIndex; // 记录每条数据在 Excel 中的绝对行号

    public ShapeImageMergeStrategy(List<QuoteExportDTO> dataList) {
        this.dataList = dataList;
        this.absRowIndex = new int[dataList.size()];
    }

    // =========================================================
    // 阶段一：写单元格时，记录真实行号 & 执行强效合并
    // =========================================================

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                 List<WriteCellData<?>> cellDataList, Cell cell, Head head,
                                 Integer relativeRowIndex, Boolean isHead) {

        if (Boolean.TRUE.equals(isHead)) return;

        int dataIdx = relativeRowIndex; // 在 dataList 中的索引
        int realRow  = cell.getRowIndex(); // Sheet 中的真实绝对行号

        // 1. 利用最先写入的列(NO.列)记录该行的真实行号
        if (cell.getColumnIndex() == 0) {
            absRowIndex[dataIdx] = realRow;
        }

        // 2. 只在图片列触发合并判断，节省性能
        if (cell.getColumnIndex() != PHOTO_COL) return;

        String curShape = dataList.get(dataIdx).getShapeCode();

        // 3. 判断是否是本组最后一行
        boolean isLast = (dataIdx == dataList.size() - 1)
                || !curShape.equals(dataList.get(dataIdx + 1).getShapeCode());

        if (!isLast) return;

        // 4. 找本组起始索引
        int startIdx = dataIdx;
        while (startIdx > 0 && curShape.equals(dataList.get(startIdx - 1).getShapeCode())) {
            startIdx--;
        }

        // 5. 跨越多行执行 Unsafe 强制合并
        if (startIdx < dataIdx) {
            int firstRow = absRowIndex[startIdx];
            int lastRow  = realRow;

            Sheet sheet = writeSheetHolder.getSheet();
            sheet.addMergedRegionUnsafe(new CellRangeAddress(firstRow, lastRow, PHOTO_COL, PHOTO_COL));

            // 设置合并首格居中对齐，让界面更美观
            Row firstSheetRow = sheet.getRow(firstRow);
            if (firstSheetRow != null) {
                Cell firstCell = firstSheetRow.getCell(PHOTO_COL);
                if (firstCell != null) {
                    CellStyle cs = sheet.getWorkbook().createCellStyle();
                    cs.cloneStyleFrom(firstCell.getCellStyle());
                    cs.setAlignment(HorizontalAlignment.CENTER);
                    cs.setVerticalAlignment(VerticalAlignment.CENTER);
                    firstCell.setCellStyle(cs);
                }
            }
        }
    }

    // =========================================================
    // 阶段二：Workbook 销毁前（数据全写完），全局绘制居中图片
    // =========================================================

    @Override
    public void beforeWorkbookCreate() {}

    @Override
    public void afterWorkbookCreate(WriteWorkbookHolder writeWorkbookHolder) {}

    @Override
    public void afterWorkbookDispose(WriteWorkbookHolder writeWorkbookHolder) {
        if (dataList == null || dataList.isEmpty()) return;

        Workbook workbook = writeWorkbookHolder.getWorkbook();
        XSSFSheet sheet = (workbook instanceof org.apache.poi.xssf.streaming.SXSSFWorkbook)
                ? ((org.apache.poi.xssf.streaming.SXSSFWorkbook) workbook).getXSSFWorkbook().getSheetAt(0)
                : (XSSFSheet) workbook.getSheetAt(0);
        XSSFDrawing drawing = sheet.createDrawingPatriarch();

        // -------------------------------------------------------------
        // 【核心黑科技】：拒绝询问 POI，使用纯数学常量计算！
        // 因为我们在 DTO 中写死了 @ContentRowHeight(110)
        // -------------------------------------------------------------
        int EMU_PER_PIXEL = 9525;
        float ROW_HEIGHT_PT = 110f;
        float ROW_HEIGHT_PX = ROW_HEIGHT_PT * 96f / 72f;
        long ROW_HEIGHT_EMU = Math.round(ROW_HEIGHT_PX * EMU_PER_PIXEL);

        int i = 0;
        while (i < dataList.size()) {
            QuoteExportDTO cur = dataList.get(i);
            byte[] photo = cur.getPhoto();

            int groupEnd = i;
            while (groupEnd + 1 < dataList.size() && dataList.get(groupEnd + 1).getShapeCode().equals(cur.getShapeCode())) {
                groupEnd++;
            }

            if (photo != null && photo.length > 0) {
                try {
                    int firstAbsRow = absRowIndex[i];
                    int lastAbsRow  = absRowIndex[groupEnd];
                    int rowCount = lastAbsRow - firstAbsRow + 1;

                    // 1. 获取合并单元格的真实宽高（物理像素）
                    double totalHeightPx = rowCount * ROW_HEIGHT_PX;
                    double colWidthPx = sheet.getColumnWidthInPixels(PHOTO_COL);

                    // 2. 读取图片的原始大小，绝不强行固定尺寸，保证原汁原味
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(photo));
                    if (img != null) {
                        double nativeW = img.getWidth();
                        double nativeH = img.getHeight();

                        // 3. 动态计算等比缩放率（留白 10%，防贴边）
                        double scale = Math.min((colWidthPx * 0.9) / nativeW, (totalHeightPx * 0.9) / nativeH);
                        double actualW_px = nativeW * scale;
                        double actualH_px = nativeH * scale;

                        // 4. 计算图片在合并大格子里的“居中偏移量”
                        double offsetX_px = (colWidthPx - actualW_px) / 2.0;
                        double offsetY_px = (totalHeightPx - actualH_px) / 2.0;

                        // 5. 将偏移量转换为距离合并区域左上角的绝对 EMU 坐标
                        long startX_emu = Math.round(offsetX_px * EMU_PER_PIXEL);
                        long startY_emu = Math.round(offsetY_px * EMU_PER_PIXEL);
                        long endX_emu = startX_emu + Math.round(actualW_px * EMU_PER_PIXEL);
                        long endY_emu = startY_emu + Math.round(actualH_px * EMU_PER_PIXEL);

                        // 6. 纯数学映射：将绝对 EMU 转换为 Excel 的【行号+列号+内部偏移】
                        int col1 = PHOTO_COL;
                        int dx1 = (int) startX_emu;
                        int col2 = PHOTO_COL; // 宽度不足一列，终点依然是这列
                        int dx2 = (int) endX_emu;

                        int row1 = firstAbsRow + (int)(startY_emu / ROW_HEIGHT_EMU);
                        int dy1 = (int)(startY_emu % ROW_HEIGHT_EMU);
                        int row2 = firstAbsRow + (int)(endY_emu / ROW_HEIGHT_EMU);
                        int dy2 = (int)(endY_emu % ROW_HEIGHT_EMU);

                        // 7. 直接绘制：不需要任何 resize，因为坐标已经完美锁死了图片的每一个角！
                        int picIdx = workbook.addPicture(photo, detectType(photo));
                        XSSFClientAnchor anchor = sheet.getWorkbook().getCreationHelper().createClientAnchor();
                        anchor.setCol1(col1); anchor.setDx1(dx1);
                        anchor.setRow1(row1); anchor.setDy1(dy1);
                        anchor.setCol2(col2); anchor.setDx2(dx2);
                        anchor.setRow2(row2); anchor.setDy2(dy2);
                        anchor.setAnchorType(org.apache.poi.ss.usermodel.ClientAnchor.AnchorType.MOVE_DONT_RESIZE);

                        drawing.createPicture(anchor, picIdx);
                    }
                } catch (Exception e) {
                    System.err.println("图片绘制失败 shapeCode=" + cur.getShapeCode() + " err=" + e.getMessage());
                }
            }
            i = groupEnd + 1;
        }
    }

    // =========================================================
    // 工具方法
    // =========================================================

    /** 等比缩放，返回目标像素 [width, height] */
    private int[] fitSize(byte[] data, int maxW, int maxH) {
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) return new int[]{maxW, maxH};
            int w = img.getWidth(), h = img.getHeight();
            if (w <= 0 || h <= 0) return new int[]{maxW, maxH};
            double ratio = Math.min((double) maxW / w, (double) maxH / h);
            return new int[]{(int)(w * ratio), (int)(h * ratio)};
        } catch (Exception e) {
            return new int[]{maxW, maxH};
        }
    }

    /** 智能嗅探图片格式头 */
    private int detectType(byte[] data) {
        if (data.length >= 2) {
            if (data[0] == (byte)0x89 && data[1] == 0x50) return Workbook.PICTURE_TYPE_PNG;
            if (data[0] == (byte)0xFF && data[1] == (byte)0xD8) return Workbook.PICTURE_TYPE_JPEG;
        }
        return Workbook.PICTURE_TYPE_JPEG;
    }
}