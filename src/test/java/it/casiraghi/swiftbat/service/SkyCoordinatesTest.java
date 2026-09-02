package it.casiraghi.swiftbat.service;

import it.casiraghi.swiftbat.model.MollweidePoint;
import it.casiraghi.swiftbat.model.SkyPoint3D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkyCoordinatesTest {
    @Test
    void centerOfMollweideIsRaZeroDecZero() {
        MollweidePoint point = SkyCoordinates.mollweide(0.0, 0.0);
        assertEquals(0.0, point.x(), 1e-12);
        assertEquals(0.0, point.y(), 1e-12);
    }

    @Test
    void positiveRaGoesLeftInAstronomicalMollweide() {
        assertTrue(SkyCoordinates.mollweide(90.0, 0.0).x() < 0.0);
    }

    @Test
    void spherePointKeepsRequestedRadius() {
        SkyPoint3D point = SkyCoordinates.onSphere(132.0, -24.0, 220.0);
        double radius = Math.sqrt(point.x() * point.x() + point.y() * point.y() + point.z() * point.z());
        assertEquals(220.0, radius, 1e-9);
    }

    @Test
    void formatsAstronomicalCoordinates() {
        assertEquals("12h 00m 00.00s", SkyCoordinates.raToHms(180.0));
        assertEquals("−30° 30′ 00.00″", SkyCoordinates.decToDms(-30.5));
    }

    @Test
    void galacticPlaneConversionReturnsFiniteEquatorialCoordinates() {
        for (int l = 0; l < 360; l += 15) {
            double[] equatorial = SkyCoordinates.galacticPlaneRaDec(l);
            assertTrue(equatorial[0] >= 0.0 && equatorial[0] < 360.0);
            assertTrue(equatorial[1] >= -90.0 && equatorial[1] <= 90.0);
        }
    }
}
