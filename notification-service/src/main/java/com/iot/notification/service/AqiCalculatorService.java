package com.iot.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AqiCalculatorService {

    // Helper class for breakpoints
    private static class Breakpoint {
        double cLow;
        double cHigh;
        int iLow;
        int iHigh;

        public Breakpoint(double cLow, double cHigh, int iLow, int iHigh) {
            this.cLow = cLow;
            this.cHigh = cHigh;
            this.iLow = iLow;
            this.iHigh = iHigh;
        }
    }

    // PM2.5 (ug/m3) - VN_AQI (QĐ 1459/QĐ-TCMT)
    private static final Breakpoint[] PM25_BREAKPOINTS = {
            new Breakpoint(0.0, 25.0, 0, 50),
            new Breakpoint(25.1, 50.0, 51, 100),
            new Breakpoint(50.1, 80.0, 101, 150),
            new Breakpoint(80.1, 150.0, 151, 200),
            new Breakpoint(150.1, 250.0, 201, 300),
            new Breakpoint(250.1, 350.0, 301, 400),
            new Breakpoint(350.1, 500.0, 401, 500)
    };

    // PM10 (ug/m3) - VN_AQI (QĐ 1459/QĐ-TCMT)
    private static final Breakpoint[] PM10_BREAKPOINTS = {
            new Breakpoint(0, 50, 0, 50),
            new Breakpoint(51, 150, 51, 100),
            new Breakpoint(151, 250, 101, 150),
            new Breakpoint(251, 350, 151, 200),
            new Breakpoint(351, 420, 201, 300),
            new Breakpoint(421, 500, 301, 400),
            new Breakpoint(501, 600, 401, 500)
    };

    // CO (ug/m3) - VN_AQI (QĐ 1459/QĐ-TCMT)
    private static final Breakpoint[] CO_BREAKPOINTS = {
            new Breakpoint(0, 10000, 0, 50),
            new Breakpoint(10001, 30000, 51, 100),
            new Breakpoint(30001, 45000, 101, 150),
            new Breakpoint(45001, 60000, 151, 200),
            new Breakpoint(60001, 90000, 201, 300),
            new Breakpoint(90001, 120000, 301, 400),
            new Breakpoint(120001, 150000, 401, 500)
    };

    // SO2 (ug/m3) - VN_AQI (QĐ 1459/QĐ-TCMT)
    private static final Breakpoint[] SO2_BREAKPOINTS = {
            new Breakpoint(0, 125, 0, 50),
            new Breakpoint(126, 350, 51, 100),
            new Breakpoint(351, 550, 101, 150),
            new Breakpoint(551, 800, 151, 200),
            new Breakpoint(801, 1600, 201, 300),
            new Breakpoint(1601, 2100, 301, 400),
            new Breakpoint(2101, 2630, 401, 500)
    };

    // NO2 (ug/m3) - VN_AQI (QĐ 1459/QĐ-TCMT)
    private static final Breakpoint[] NO2_BREAKPOINTS = {
            new Breakpoint(0, 100, 0, 50),
            new Breakpoint(101, 200, 51, 100),
            new Breakpoint(201, 700, 101, 150),
            new Breakpoint(701, 1200, 151, 200),
            new Breakpoint(1201, 2350, 201, 300),
            new Breakpoint(2351, 3100, 301, 400),
            new Breakpoint(3101, 3850, 401, 500)
    };

    // O3 (ug/m3) 1h avg - VN_AQI (QĐ 1459/QĐ-TCMT)
    private static final Breakpoint[] O3_BREAKPOINTS = {
            new Breakpoint(0, 160, 0, 50),
            new Breakpoint(161, 200, 51, 100),
            new Breakpoint(201, 300, 101, 150),
            new Breakpoint(301, 400, 151, 200),
            new Breakpoint(401, 800, 201, 300),
            new Breakpoint(801, 1000, 301, 400),
            new Breakpoint(1001, 1200, 401, 500)
    };

    /**
     * Tnh Sub-AQI cho mmht loami kh/bmi.
     */
    public Integer calculateSubAqi(String pollutantCode, double concentration) {
        if (concentration < 0) return null;

        Breakpoint[] breakpoints = null;
        String upperCode = pollutantCode.toUpperCase();
        
        if (upperCode.contains("PM2.5") || upperCode.contains("PM25")) {
            breakpoints = PM25_BREAKPOINTS;
        } else if (upperCode.contains("PM10")) {
            breakpoints = PM10_BREAKPOINTS;
        } else if (upperCode.contains("CO")) {
            breakpoints = CO_BREAKPOINTS;
        } else if (upperCode.contains("SO2")) {
            breakpoints = SO2_BREAKPOINTS;
        } else if (upperCode.contains("NO2")) {
            breakpoints = NO2_BREAKPOINTS;
        } else if (upperCode.contains("O3") || upperCode.contains("OZ") || upperCode.contains("OZONE")) {
            breakpoints = O3_BREAKPOINTS;
        } else {
            log.debug("Pollutant {} is not supported for AQI calculation.", pollutantCode);
            return null;
        }

        for (Breakpoint bp : breakpoints) {
            if (concentration >= bp.cLow && concentration <= bp.cHigh) {
                // Formula: AQI = ((IHigh - ILow) / (CHigh - CLow)) * (C - CLow) + ILow
                double aqi = ((double)(bp.iHigh - bp.iLow) / (bp.cHigh - bp.cLow)) * (concentration - bp.cLow) + bp.iLow;
                return (int) Math.round(aqi);
            }
        }

        // If concentration exceeds highest breakpoint, extrapolate or cap at 500. We cap at 500 for simplicity.
        if (concentration > breakpoints[breakpoints.length - 1].cHigh) {
            return 500;
        }

        return null;
    }

    public String getAqiLevel(int aqiValue) {
        if (aqiValue <= 50) return "Tốt";
        if (aqiValue <= 100) return "Trung bình";
        if (aqiValue <= 150) return "Kém";
        if (aqiValue <= 200) return "Xấu";
        if (aqiValue <= 300) return "Rất xấu";
        return "Nguy hại";
    }
}
