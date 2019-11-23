package de.codefor.le.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
public class PoliceTickerControllerTest {

    @Autowired
    PoliceTickerController controller;

    @Test
    public void getx() {
        assertThat(controller.getx(PageRequest.of(0, 1))).isNotNull().isEmpty();
    }

    @Test
    public void getLocations() {
        assertThat(controller.getLocations("Lindenau")).isNotNull().isEmpty();
        assertThat(controller.getLocations("Zentrum")).isNotNull().isEmpty();
    }

    @Test
    public void search() {
        final var result = controller.search("term", PageRequest.of(0, 1));
        assertThat(result).isNotNull();
        assertThat(result.getNumberOfElements()).isZero();
    }

    @Test
    public void searchBetween() {
        final var result = controller.searchBetween("term", LocalDateTime.now().minus(1, ChronoUnit.DAYS),
                LocalDateTime.now(), PageRequest.of(0, 1));
        assertThat(result).isNotNull();
        assertThat(result.getNumberOfElements()).isLessThanOrEqualTo(1);
    }

    @Test
    public void minMaxDate() {
        assertThat(controller.minMaxDate()).isNotNull().hasSize(2);
    }

    @Test
    public void last7Days() {
        final var result = controller.last7Days();
        assertThat(result).isNotNull().hasSize(2);
        assertThat(result[1].getDayOfMonth()).isEqualTo(DateTime.now().getDayOfMonth());
    }
}
