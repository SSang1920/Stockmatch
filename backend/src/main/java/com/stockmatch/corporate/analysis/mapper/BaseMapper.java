package com.stockmatch.corporate.analysis.mapper;

public abstract class BaseMapper {

    protected long parseLong(String value){
        try{
            if(value == null || value.equalsIgnoreCase("None") || value.equals("-")){
                return 0L;
            }
            return Long.parseLong(value.trim());
        } catch(NumberFormatException e) {
            return 0L;
        }
    }

    protected Double parseDouble(String value) {
        try{
            if(value == null || value.equalsIgnoreCase("None") || value.equals("-")){
                return null;
            }
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected double calculateRatio(Long numerator, Long denominator) {
        if(denominator == null || denominator == 0){
            return 0.0;
        }
        return (double) numerator / denominator;
    }

    protected Double calculateGrowth(Long current, Long previous){
        if(previous ==null || previous == 0) {
            return null;
        }
        return (double) (current - previous) / previous;
    }
}
