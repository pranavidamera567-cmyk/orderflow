package com.orderflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FulfillmentFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    JsonNode json(String body) throws Exception {
        return mapper.readTree(body);
    }

    JsonNode placeOrder(String sku, int qty) throws Exception {
        String body = """
                {"customerEmail":"a@b.com","items":[{"sku":"%s","quantity":%d}]}""".formatted(sku, qty);
        return json(mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    JsonNode inventory(String sku) throws Exception {
        JsonNode all = json(mvc.perform(get("/api/inventory")).andReturn().getResponse().getContentAsString());
        for (JsonNode n : all) {
            if (n.get("sku").asText().equals(sku)) {
                return n;
            }
        }
        throw new AssertionError("no sku " + sku);
    }

    JsonNode shipmentOf(String orderId) throws Exception {
        return json(mvc.perform(get("/api/shipments/by-order/" + orderId)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    String orderStatus(String orderId) throws Exception {
        return json(mvc.perform(get("/api/orders/" + orderId)).andReturn().getResponse().getContentAsString())
                .get("status").asText();
    }

    @Test
    void orderIsConfirmedReservesStockAndCreatesShipment() throws Exception {
        int before = inventory("SKU-1001").get("available").asInt();
        JsonNode order = placeOrder("SKU-1001", 3);
        assertEquals("CONFIRMED", order.get("status").asText());
        assertEquals(before - 3, inventory("SKU-1001").get("available").asInt());
        assertEquals("CREATED", shipmentOf(order.get("id").asText()).get("status").asText());
    }

    @Test
    void insufficientStockIsRejectedAndLeavesInventoryUntouched() throws Exception {
        int before = inventory("SKU-1004").get("available").asInt();
        String body = """
                {"customerEmail":"a@b.com","items":[{"sku":"SKU-1004","quantity":%d}]}""".formatted(before + 1);
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        assertEquals(before, inventory("SKU-1004").get("available").asInt());
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerEmail\":\"nope\",\"items\":[]}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerEmail\":\"a@b.com\",\"items\":[{\"sku\":\"NOPE\",\"quantity\":1}]}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shipmentScansDriveOrderToDeliveredAndCommitStock() throws Exception {
        int onHand = inventory("SKU-1002").get("onHand").asInt();
        String orderId = placeOrder("SKU-1002", 2).get("id").asText();
        String shipmentId = shipmentOf(orderId).get("id").asText();

        mvc.perform(post("/api/shipments/" + shipmentId + "/advance")).andExpect(status().isOk());
        assertEquals("SHIPPED", orderStatus(orderId));
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/shipments/" + shipmentId + "/advance")).andExpect(status().isOk());
        }
        assertEquals("DELIVERED", orderStatus(orderId));
        assertEquals(onHand - 2, inventory("SKU-1002").get("onHand").asInt());
        mvc.perform(post("/api/shipments/" + shipmentId + "/advance")).andExpect(status().isConflict());
        mvc.perform(post("/api/orders/" + orderId + "/cancel")).andExpect(status().isConflict());
        mvc.perform(get("/api/shipments/" + shipmentId)).andExpect(jsonPath("$.history.length()").value(5));
    }

    @Test
    void exceptionCanBeRaisedAndResolved() throws Exception {
        String orderId = placeOrder("SKU-1001", 1).get("id").asText();
        String shipmentId = shipmentOf(orderId).get("id").asText();
        mvc.perform(post("/api/shipments/" + shipmentId + "/advance")).andExpect(status().isOk());

        mvc.perform(post("/api/shipments/" + shipmentId + "/exception")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Address invalid\"}"))
                .andExpect(status().isOk());
        assertEquals("EXCEPTION", orderStatus(orderId));

        mvc.perform(post("/api/shipments/" + shipmentId + "/resolve")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"));
        assertEquals("SHIPPED", orderStatus(orderId));
    }

    @Test
    void cancellingReleasesStockAndCancelsShipment() throws Exception {
        int before = inventory("SKU-1003").get("available").asInt();
        String orderId = placeOrder("SKU-1003", 4).get("id").asText();
        mvc.perform(post("/api/orders/" + orderId + "/cancel")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(before, inventory("SKU-1003").get("available").asInt());
        assertEquals("CANCELLED", shipmentOf(orderId).get("status").asText());
    }
}
