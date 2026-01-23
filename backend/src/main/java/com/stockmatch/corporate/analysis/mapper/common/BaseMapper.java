package com.stockmatch.corporate.analysis.mapper.common;

public abstract class BaseMapper {

    protected long parseLong(String value){
        try{
            if(value == null || value.equalsIgnoreCase("None") || value.equals("-") || value.trim().isEmpty()){
                return 0L;
            }
            String cleanValue = value.replace(",","").trim();
            return Long.parseLong(cleanValue);
        } catch(NumberFormatException e) {
            return 0L;
        }
    }

    protected Double parseDouble(String value) {
        try{
            if(value == null || value.equalsIgnoreCase("None") || value.equals("-")){
                return null;
            }
            String cleanValue = value.replace(",","").trim();
            return Double.parseDouble(cleanValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    protected double calculateRatio(Long numerator, Long denominator){
        if (numerator == null || denominator == null || denominator == 0) {
            return 0.0;
        }
        return (double) numerator / denominator;
    }

    //일반 성장률
    protected Double calculateGrowth(Long current, Long previous) {
        if (current == null || previous == null || previous == 0){
            return null;
        }
        return (double) (current -previous) / previous;
    }

    //적자 개선 반영 성장률
    protected Double calculateGrowthWithAbs(Long current, Long previous) {
        if (current == null || previous == null || previous == 0) {
            return null;
        }
        return (double) (current -previous) / Math.abs(previous);
    }
}
