import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

//This class represents a single working shift
class Shift{
	Calendar start;
	Calendar end;
}

//This class has the employee name and list of shifts he/she worked
class EmployeeShiftDetailRecords{
	
	String employeeName;
	ArrayList<Shift> shifts;
	
	//Constructor
	EmployeeShiftDetailRecords(String name){
		employeeName=name;
		shifts=new ArrayList<>();
	}
	
	//Function to add a shift the records
	public void addShift(Date startDate,Date endDate) {
		Shift shift=new Shift();
		shift.start=Calendar.getInstance();
		shift.start.setTime(startDate);
		shift.end=Calendar.getInstance();
		shift.end.setTime(endDate);
		shifts.add(shift);
	}
	
}

//The main class
class DataAnalysis {
	
	//A logger to log info/stages
	private static final Logger logger = LogManager.getLogger(DataAnalysis.class);
	
    public static void main(String args[]) {
        try {
            File inputFile = new File("../Tasks/src/main/java/Assignment_Timecard.xlsx");
            logger.info("File successfully fetched");
            analyzeEmployeeData(inputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void analyzeEmployeeData(File inputFile) throws Exception {
        FileInputStream fis = new FileInputStream(inputFile);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);
        boolean isHeading=true;
        
        //A hashmap that keep Employee ID as key and shift records as value.
        Map<String,EmployeeShiftDetailRecords> records=new HashMap<>(); 
        
        for (Row row : sheet) {
        	
        	//Skipping the first row.
        	if(isHeading) {
        		isHeading=false;
        		continue;
        	}
        	
    		String employeeId=row.getCell(0).getStringCellValue();
    		String employeeName=row.getCell(7).getStringCellValue();
    		
    		//Add new employee to record if he/she appears for the first time.
    		if(!records.containsKey(employeeId)) records.put(employeeId,new EmployeeShiftDetailRecords(employeeName));
    		
    		//If cells are not empty, add the shift the records.
    		if(row.getCell(2).getCellType().equals(CellType.NUMERIC)&&row.getCell(3).getCellType().equals(CellType.NUMERIC)){
    			records.get(employeeId).addShift(row.getCell(2).getDateCellValue(), row.getCell(3).getDateCellValue());
    		}
        		
        }
        logger.info("Data fetched from the .xlsx file sucessfully");
        logger.info("Analysing data...");
        //All the data added the hashmap.
        
        
        //Sorting the shifts based on start time so if the are jumbled they can get back to a straight timeline.
        preProcessing(records);
        
        //Function for each task
        workedFor7ConsecutiveDays(records);
        workGapMoreThan1andLessThan10(records);
        workedMorethan14hours(records);
        System.out.println("\n");
        
        //Closing workbook after completion
        workbook.close();
        logger.info("Workbook closed");
    }
    
    public static void preProcessing(Map<String,EmployeeShiftDetailRecords> records) {
    	for(String id:records.keySet()) {
    		Collections.sort(records.get(id).shifts, Comparator.comparing(shift -> shift.start));
    	}
    }

    public static void workedFor7ConsecutiveDays(Map<String,EmployeeShiftDetailRecords> records) {
    	
    	System.out.println("\n\nList of employees who worked for 7 consecutive days");
    	
    	int srno=1;
    	
    	for(String id:records.keySet()) {
    		ArrayList<Shift> shifts=records.get(id).shifts;
    		int count,n=shifts.size();
    		
    		if(n==0) continue; // Empty list-no shifts.
    		
    		if(isSameDay(shifts.get(0).start,shifts.get(0).end)) count=1;
    		else count=2;
    		
    		for(int i=1;i<n;i++) {
    			
    			if(isSameDay(shifts.get(i-1).end,shifts.get(i).start));    			
    			else if(isConsecutiveDays(shifts.get(i-1).end,shifts.get(i).start)) count++;
    			else count=1;
    			
    			if(isConsecutiveDays(shifts.get(i).start,shifts.get(i).end)) count++;
    			
    			if(count>=7) {
    				System.out.println(srno+". "+id+" : "+records.get(id).employeeName);
    				srno++;
    				break;//Employee completed 7 consecutive days.
    			}
    		}
    	}
    }
    
    //Function to check whether 2 days are same
    public static boolean isSameDay(Calendar day1,Calendar day2) {
    	return day1.get(Calendar.YEAR)==day2.get(Calendar.YEAR)&&
    			day1.get(Calendar.MONTH)==day2.get(Calendar.MONTH)&&
    			day1.get(Calendar.DAY_OF_MONTH)==day2.get(Calendar.DAY_OF_MONTH);
    }
    
    //Function to check whether 2 days are consecutive
    public static boolean isConsecutiveDays(Calendar day1,Calendar day2) {
    	Calendar temp=Calendar.getInstance();
    	temp.setTimeInMillis(day1.getTimeInMillis());
    	
    	//Add 24 hours to the first day and checking if it matches the second day.
    	temp.add(Calendar.HOUR_OF_DAY, 24);
    	return isSameDay(temp,day2);
    }

    public static void workGapMoreThan1andLessThan10(Map<String,EmployeeShiftDetailRecords> records) {
    	
    	System.out.println("\n\nList of employees who have less than 10 hours of time between shifts but greater than 1 hour");
    	
    	int srno=1;
    	
    	for(String id:records.keySet()) {
    		
    		ArrayList<Shift> shifts=records.get(id).shifts;
    		
    		for(int i=1;i<shifts.size();i++) {
    			
    			//Time duration between 2 shifts
    			long timeGap=(shifts.get(i).start.getTimeInMillis()-shifts.get(i-1).end.getTimeInMillis())/(60*60*1000);
    			
    			//Assuming both 1 and 10 are inclusive.
    			if(timeGap>=1&&timeGap<=10) {
    				System.out.println(srno+". "+id+" : "+records.get(id).employeeName);
    				srno++;
    				break;
    			}
    		}
    	}

    }

    
    public static void workedMorethan14hours(Map<String,EmployeeShiftDetailRecords> records) {
    	
    	System.out.println("\n\nList of employees who has worked for more than 14 hours in a single shift with count");
    	
    	int srno=1;
    	
    	for(String id:records.keySet()) {
    		
    		boolean conditionSatisfied=false;
    		int count=0;//Number of times he worked for more than 14 hours.
    		
    		for(Shift shift:records.get(id).shifts) {
    			
    			//Calculating shift duration
    			long timeSpent=(shift.end.getTimeInMillis()-shift.start.getTimeInMillis())/(60*60*1000);
    			
    			if(timeSpent>=14) {
    				conditionSatisfied=true;
    				count++;
    			}
    		}
    		
    		if(conditionSatisfied) {
    			System.out.println(srno+". "+id+" : "+records.get(id).employeeName+" : "+count);
    			srno++;
    		}
    	}
    }
}