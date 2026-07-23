package com.stonebridge.quotesystem.business.strategy;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.RowWriteHandler;
import com.alibaba.excel.write.handler.WorkbookWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import com.stonebridge.quotesystem.business.entity.dto.QuoteExportDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 终极修复版 v5 —— 彻底解决 drawing1.xml 损坏问题
 *
 * ===================== 根本原因（最终确认）=====================
 *
 * POI createPicture() 内部调用 createXfrm() 计算 spPr/xfrm/ext 的 cx/cy（图片渲染尺寸）：
 *
 *   cy = Σ ImageUtils.getRowHeightInPixels(xssfSheet, r) [r1..r2) - dy1 + dy2
 *
 * 问题：我们操作的是底层 XSSFSheet，它的行对象从未被写入过任何行高数据
 *       （行数据全在 SXSSFWorkbook 的流式层，XSSFSheet 行为空壳），
 *       所以 getRowHeightInPixels 读到的是 XSSFSheet 的 defaultRowHeight = 15pt。
 *
 * 而我们的 dy1/dy2 是按实际行高（60pt）计算的，当 dy1 较大时：
 *   cy = 3行 × 15pt的EMU(190500) - 716444 + 45556 = -99388 < 0 → Excel 报损坏删除图形！
 *
 * ===================== 修复方案 =====================
 *
 * 在 afterWorkbookDispose 绘图前，先把底层 XSSFSheet 的 defaultRowHeight
 * 设置为实际内容行高，这样 createXfrm() 就能读到正确的行高，cy 始终为正数。
 *
 * 缓存的每行实际行高（由 afterRowDispose 读取）用于：
 *   1. 计算 dy1/dy2（锚点坐标）
 *   2. 在绘图前设置 xssfSheet.setDefaultRowHeight()，让 createXfrm 读到正确值
 */
public class ShapeImageMergeStrategy implements CellWriteHandler, RowWriteHandler, WorkbookWriteHandler {

    private final List<QuoteExportDTO> dataList;
    private static final int PHOTO_COL     = 3;
    private static final int MAX_ROWS      = 2000;
    private static final int EMU_PER_PIXEL = 9525;

    private final int[]   absRowIndex;  // dataIdx -> sheet 绝对行号
    private final float[] rowHeightPt;  // absRow  -> 实际行高（pt）
    // 记录内容行的典型行高，用于设置 xssfSheet defaultRowHeight
    private float contentRowHeightPt = 60f; // 兜底默认值

    public ShapeImageMergeStrategy(List<QuoteExportDTO> dataList) {
        this.dataList    = dataList;
        this.absRowIndex = new int[dataList.size()];
        this.rowHeightPt = new float[MAX_ROWS];
        Arrays.fill(rowHeightPt, 60f);
    }

    // =========================================================
    // 阶段零：每行写完后，缓存真实行高
    // =========================================================

    @Override
    public void afterRowDispose(WriteSheetHolder writeSheetHolder,
                                WriteTableHolder writeTableHolder,
                                Row row,
                                Integer relativeRowIndex,
                                Boolean isHead) {
        if (row == null) return;
        int absRow = row.getRowNum();
        if (absRow >= 0 && absRow < MAX_ROWS) {
            float pt = row.getHeightInPoints();
            if (pt > 0) {
                rowHeightPt[absRow] = pt;
                // 记录内容行高（非表头）
                if (!Boolean.TRUE.equals(isHead)) {
                    contentRowHeightPt = pt;
                }
            }
        }
    }

    // =========================================================
    // 阶段一：写单元格时，记录真实行号 & 执行合并
    // =========================================================

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                 List<WriteCellData<?>> cellDataList, Cell cell, Head head,
                                 Integer relativeRowIndex, Boolean isHead) {
        if (Boolean.TRUE.equals(isHead)) return;

        int dataIdx = relativeRowIndex;
        int realRow = cell.getRowIndex();

        if (cell.getColumnIndex() == 0) {
            absRowIndex[dataIdx] = realRow;
        }

        if (cell.getColumnIndex() != PHOTO_COL) return;

        String curShape = dataList.get(dataIdx).getShapeCode();
        boolean isLast  = (dataIdx == dataList.size() - 1)
                || !Objects.equals(curShape, dataList.get(dataIdx + 1).getShapeCode());
        if (!isLast) return;

        int startIdx = dataIdx;
        while (startIdx > 0 && Objects.equals(curShape, dataList.get(startIdx - 1).getShapeCode())) {
            startIdx--;
        }

        if (startIdx < dataIdx) {
            int firstRow = absRowIndex[startIdx];
            int lastRow  = realRow;
            Sheet sheet  = writeSheetHolder.getSheet();
            sheet.addMergedRegionUnsafe(new CellRangeAddress(firstRow, lastRow, PHOTO_COL, PHOTO_COL));

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
    // 阶段二：Workbook 销毁前，绘制图片
    // =========================================================

    @Override
    public void beforeWorkbookCreate() {}

    @Override
    public void afterWorkbookCreate(WriteWorkbookHolder writeWorkbookHolder) {}

    @Override
    public void afterWorkbookDispose(WriteWorkbookHolder writeWorkbookHolder) {
        if (dataList == null || dataList.isEmpty()) return;

        Workbook workbook = writeWorkbookHolder.getWorkbook();

        XSSFWorkbook xssfWorkbook;
        if (workbook instanceof SXSSFWorkbook) {
            xssfWorkbook = ((SXSSFWorkbook) workbook).getXSSFWorkbook();
        } else if (workbook instanceof XSSFWorkbook) {
            xssfWorkbook = (XSSFWorkbook) workbook;
        } else {
            System.err.println("[ShapeImageMergeStrategy] 不支持的 Workbook 类型");
            return;
        }
        XSSFSheet xssfSheet = xssfWorkbook.getSheetAt(0);

        // ==============================================================
        // 【核心修复】在绘图前，把底层 XSSFSheet 的 defaultRowHeight 设置为
        // 实际内容行高。
        //
        // 原因：POI createPicture() 内部的 createXfrm() 会调用：
        //   ImageUtils.getRowHeightInPixels(xssfSheet, row)
        // 对于 SXSSFWorkbook，底层 XSSFSheet 的行对象是空的（行数据在流式层），
        // 所以它读到的是 xssfSheet.getDefaultRowHeightInPoints() = 15pt（Excel默认）。
        // 用 15pt 计算出的 cy 可能为负数 → Excel 报修复错误并删除该图形。
        //
        // 设置 defaultRowHeight 后，getRowHeightInPixels 对空行也能读到正确值。
        // ==============================================================
        short contentRowHeightInUnits = (short) Math.round(contentRowHeightPt * 20); // 单位：1/20 pt
        xssfSheet.setDefaultRowHeight(contentRowHeightInUnits);

        XSSFDrawing drawing = xssfSheet.getDrawingPatriarch();
        if (drawing == null) {
            drawing = xssfSheet.createDrawingPatriarch();
        }

        int i = 0;
        while (i < dataList.size()) {
            QuoteExportDTO cur   = dataList.get(i);
            byte[]         photo = cur.getPhoto();

            int groupEnd = i;
            while (groupEnd + 1 < dataList.size()
                    && Objects.equals(dataList.get(groupEnd + 1).getShapeCode(), cur.getShapeCode())) {
                groupEnd++;
            }

            if (photo != null && photo.length > 0) {
                try {
                    drawPicture(xssfSheet, xssfWorkbook, drawing, photo, i, groupEnd);
                } catch (Exception e) {
                    System.err.println("[ShapeImageMergeStrategy] 图片绘制失败 shapeCode="
                            + cur.getShapeCode() + " err=" + e.getMessage());
                }
            }
            i = groupEnd + 1;
        }
    }

    // =========================================================
    // 核心绘图
    // =========================================================

    private void drawPicture(XSSFSheet xssfSheet, XSSFWorkbook xssfWorkbook,
                             XSSFDrawing drawing,
                             byte[] photo, int groupStart, int groupEnd) throws Exception {

        int firstAbsRow = absRowIndex[groupStart];
        int lastAbsRow  = absRowIndex[groupEnd];

        // 逐行累加真实行高
        double totalHeightPx = 0;
        for (int r = firstAbsRow; r <= lastAbsRow; r++) {
            float pt = (r < MAX_ROWS) ? rowHeightPt[r] : contentRowHeightPt;
            totalHeightPx += pt * 96.0 / 72.0;
        }
        double colWidthPx = xssfSheet.getColumnWidthInPixels(PHOTO_COL);

        java.awt.image.BufferedImage img =
                javax.imageio.ImageIO.read(new ByteArrayInputStream(photo));
        if (img == null) return;

        double nativeW = img.getWidth();
        double nativeH = img.getHeight();
        double scale   = Math.min((colWidthPx * 0.9) / nativeW, (totalHeightPx * 0.9) / nativeH);
        double actualW = nativeW * scale;
        double actualH = nativeH * scale;

        double offsetX_px = (colWidthPx - actualW) / 2.0;
        double offsetY_px = (totalHeightPx - actualH) / 2.0;

        // X 方向
        long colWidthEmu = Math.round(colWidthPx * EMU_PER_PIXEL);
        long startX_emu  = Math.round(offsetX_px * EMU_PER_PIXEL);
        long endX_emu    = startX_emu + Math.round(actualW * EMU_PER_PIXEL);
        int dx1  = (int) Math.max(0, Math.min(startX_emu, colWidthEmu - 1));
        int col2 = PHOTO_COL;
        int dx2;
        if (endX_emu <= colWidthEmu) {
            dx2 = (int) endX_emu;
        } else {
            col2 = PHOTO_COL + 1;
            dx2  = (int) (endX_emu - colWidthEmu);
        }

        // Y 方向（逐行累加，dy 严格 < 该行行高 EMU）
        long[] fromY = yToRowOffset(firstAbsRow, lastAbsRow, offsetY_px);
        long[] toY   = yToRowOffset(firstAbsRow, lastAbsRow, offsetY_px + actualH);
        int row1 = (int) fromY[0];
        int dy1  = (int) fromY[1];
        int row2 = (int) toY[0];
        int dy2  = (int) toY[1];

        // 防御
        if (row1 > row2 || (row1 == row2 && col2 == PHOTO_COL && dy1 >= dy2)) {
            System.err.println("[ShapeImageMergeStrategy] 锚点异常跳过: row1="
                    + row1 + " dy1=" + dy1 + " row2=" + row2 + " dy2=" + dy2);
            return;
        }

        int picIdx = xssfWorkbook.addPicture(photo, detectType(photo));
        XSSFClientAnchor anchor = new XSSFClientAnchor(
                dx1, dy1, dx2, dy2,
                PHOTO_COL, row1, col2, row2
        );
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
        drawing.createPicture(anchor, picIdx);
    }

    /**
     * 将相对于 firstAbsRow 顶部的 Y 像素偏移，映射为 (absRowIndex, rowOffsetEMU)。
     * 确保 rowOffsetEMU 严格 < 该行行高 EMU（OOXML 合法性要求）。
     */
    private long[] yToRowOffset(int firstAbsRow, int lastAbsRow, double yPx) {
        double remaining = yPx;
        for (int r = firstAbsRow; r <= lastAbsRow; r++) {
            float  pt     = (r < MAX_ROWS) ? rowHeightPt[r] : contentRowHeightPt;
            double rowHPx = pt * 96.0 / 72.0;
            if (remaining <= rowHPx || r == lastAbsRow) {
                long rowHEmu   = Math.round(rowHPx * EMU_PER_PIXEL);
                long rowOffEmu = Math.round(Math.max(0, remaining) * EMU_PER_PIXEL);
                rowOffEmu = Math.min(rowOffEmu, rowHEmu - 1);
                return new long[]{r, rowOffEmu};
            }
            remaining -= rowHPx;
        }
        float pt     = (lastAbsRow < MAX_ROWS) ? rowHeightPt[lastAbsRow] : contentRowHeightPt;
        long rowHEmu = Math.round(pt * 96.0 / 72.0 * EMU_PER_PIXEL);
        return new long[]{lastAbsRow, rowHEmu - 1};
    }

    private int detectType(byte[] data) {
        if (data.length >= 2) {
            if (data[0] == (byte) 0x89 && data[1] == 0x50) return Workbook.PICTURE_TYPE_PNG;
            if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8) return Workbook.PICTURE_TYPE_JPEG;
        }
        return Workbook.PICTURE_TYPE_JPEG;
    }
}
