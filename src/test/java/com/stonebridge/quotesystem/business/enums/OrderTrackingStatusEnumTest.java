package com.stonebridge.quotesystem.business.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTrackingStatusEnumTest {

    @Test
    void shouldUseNewDesignConfirmationStatusesAndRemoveCheckedOk() {
        assertStatus(StickerStatusEnum.values(), 3250);
        assertStatus(InnerBoxStatusEnum.values(), 5250);
        assertStatus(ColorBoxStatusEnum.values(), 7250);
        assertStatus(CartonStatusEnum.values(), 1250);

        assertMissing(StickerStatusEnum.values(), 3500);
        assertMissing(InnerBoxStatusEnum.values(), 5500);
        assertMissing(ColorBoxStatusEnum.values(), 7500);
        assertMissing(CartonStatusEnum.values(), 1500);
    }

    private void assertStatus(TrackStatusEnum[] values, Integer code) {
        TrackStatusEnum status = Arrays.stream(values)
                .filter(item -> code.equals(item.getCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("设计初步完成，已发客户待确认", status.getLabel());
    }

    private void assertMissing(TrackStatusEnum[] values, Integer code) {
        assertFalse(Arrays.stream(values).anyMatch(item -> code.equals(item.getCode())));
        assertTrue(Arrays.stream(values).allMatch(item -> item.getCode() != null));
    }
}
