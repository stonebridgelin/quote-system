package com.stonebridge.quotesystem.business.strategy;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.stonebridge.quotesystem.business.entity.dto.QuoteExportDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 将一个 SET 展开的多条组件行按父明细合并公共列。
 * 组件编号、描述、图片、设计、组件数量、重量和吨价保持逐行独立。
 */
public class QuoteSetMergeStrategy implements CellWriteHandler {

    private static final Set<String> SHARED_FIELDS = Set.of(
            "itemIndex", "setsPerCtn", "pcs", "ttlPcs", "ctns",
            "cbmCtn", "cbmTotal", "nwCtn", "nwTotal", "gwCtn", "gwTotal",
            "unitPrice", "amount", "dimension", "extraPrice", "cartonWeight", "remarks"
    );

    private final List<QuoteExportDTO> dataList;

    public QuoteSetMergeStrategy(List<QuoteExportDTO> dataList) {
        this.dataList = dataList;
    }

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder,
                                 WriteTableHolder writeTableHolder,
                                 List<WriteCellData<?>> cellDataList,
                                 Cell cell,
                                 Head head,
                                 Integer relativeRowIndex,
                                 Boolean isHead) {
        if (Boolean.TRUE.equals(isHead) || head == null || relativeRowIndex == null
                || relativeRowIndex < 0 || relativeRowIndex >= dataList.size()) {
            return;
        }

        QuoteExportDTO current = dataList.get(relativeRowIndex);
        if (!Boolean.TRUE.equals(current.getSetLine()) || !SHARED_FIELDS.contains(head.getFieldName())
                || !isLastRowOfSet(relativeRowIndex, current.getQuoteLineKey())) {
            return;
        }

        int startIndex = findSetStart(relativeRowIndex, current.getQuoteLineKey());
        if (startIndex == relativeRowIndex) {
            return;
        }

        int firstRow = cell.getRowIndex() - (relativeRowIndex - startIndex);
        Sheet sheet = writeSheetHolder.getSheet();
        sheet.addMergedRegionUnsafe(new CellRangeAddress(
                firstRow, cell.getRowIndex(), cell.getColumnIndex(), cell.getColumnIndex()));
    }

    private boolean isLastRowOfSet(int index, String quoteLineKey) {
        return index == dataList.size() - 1
                || !Objects.equals(quoteLineKey, dataList.get(index + 1).getQuoteLineKey());
    }

    private int findSetStart(int index, String quoteLineKey) {
        int start = index;
        while (start > 0 && Objects.equals(quoteLineKey, dataList.get(start - 1).getQuoteLineKey())) {
            start--;
        }
        return start;
    }
}
