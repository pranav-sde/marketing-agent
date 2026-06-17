package com.marketingagent.webclient.whatsapp;

import java.util.List;
import java.util.Map;

public record WhatsAppMessageResponse(
        String messagingProduct,
        List<Map<String, Object>> contacts,
        List<Map<String, Object>> messages
) {
}
