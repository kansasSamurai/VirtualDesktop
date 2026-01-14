# File Organizer POC - Java Program

## Overview
This Java proof of concept demonstrates a file organization system that:
1. Creates hierarchical directory structures based on object properties
2. Generates sample text files in those directories
3. Creates ZIP archives of each location folder

## Files Included
- `LocationData.java` - Data model class representing locations
- `FileOrganizerPOC.java` - Main program with all logic

## How to Compile
```bash
javac LocationData.java FileOrganizerPOC.java
```

## How to Run
```bash
java FileOrganizerPOC
```

## What It Does

### Step 1: Sample Data Creation
The program creates 4 sample LocationData objects with the following properties:
1. year=2024, agencyId=AG001, companyId=CO001, locationId=LOC001, locationName=Downtown_Office
2. year=2024, agencyId=AG001, companyId=CO002, locationId=LOC002, locationName=Westside_Branch
3. year=2023, agencyId=AG002, companyId=CO001, locationId=LOC003, locationName=North_Campus
4. year=2023, agencyId=AG002, companyId=CO003, locationId=LOC004, locationName=East_Facility

### Step 2: Directory and File Creation
For each object, the program:
- **Creates a directory path**: `root/{year}/abc/{agencyId}/{companyId}/{locationId}`
  - Example: `root/2024/abc/AG001/CO001/LOC001`
  
- **Creates a text file**: `{locationName}_{year}.txt`
  - Example: `Downtown_Office_2024.txt`
  - Contains sample data about the location

### Step 3: ZIP Archive Creation
For each locationId folder, the program creates a ZIP file containing all files in that directory.
- ZIP files are created at: `root/{year}/abc/{agencyId}/{companyId}/{locationId}.zip`
- Example: `root/2024/abc/AG001/CO001/LOC001.zip`

## Expected Output

### Console Output
```
Starting File Organizer POC...

Processing: LocationData{year=2024, agencyId='AG001', companyId='CO001', locationId='LOC001', locationName='Downtown_Office'}
  Created directory: root/2024/abc/AG001/CO001/LOC001
  Created file: root/2024/abc/AG001/CO001/LOC001/Downtown_Office_2024.txt

Processing: LocationData{year=2024, agencyId='AG001', companyId='CO002', locationId='LOC002', locationName='Westside_Branch'}
  Created directory: root/2024/abc/AG001/CO002/LOC002
  Created file: root/2024/abc/AG001/CO002/LOC002/Westside_Branch_2024.txt

Processing: LocationData{year=2023, agencyId='AG002', companyId='CO001', locationId='LOC003', locationName='North_Campus'}
  Created directory: root/2023/abc/AG002/CO001/LOC003
  Created file: root/2023/abc/AG002/CO001/LOC003/North_Campus_2023.txt

Processing: LocationData{year=2023, agencyId='AG002', companyId='CO003', locationId='LOC004', locationName='East_Facility'}
  Created directory: root/2023/abc/AG002/CO003/LOC004
  Created file: root/2023/abc/AG002/CO003/LOC004/East_Facility_2023.txt


=== Creating ZIP files ===

Created ZIP: root/2024/abc/AG001/CO001/LOC001.zip
Created ZIP: root/2024/abc/AG001/CO002/LOC002.zip
Created ZIP: root/2023/abc/AG002/CO001/LOC003.zip
Created ZIP: root/2023/abc/AG002/CO003/LOC004.zip

=== POC Completed Successfully ===
```

### File Structure Created
```
root/
├── 2023/
│   └── abc/
│       └── AG002/
│           ├── CO001/
│           │   ├── LOC003/
│           │   │   └── North_Campus_2023.txt
│           │   └── LOC003.zip
│           └── CO003/
│               ├── LOC004/
│               │   └── East_Facility_2023.txt
│               └── LOC004.zip
└── 2024/
    └── abc/
        └── AG001/
            ├── CO001/
            │   ├── LOC001/
            │   │   └── Downtown_Office_2024.txt
            │   └── LOC001.zip
            └── CO002/
                ├── LOC002/
                │   └── Westside_Branch_2024.txt
                └── LOC002.zip
```

## Key Features Demonstrated

1. **Dynamic Path Construction**: Uses String.format() to build paths with variable values
2. **Directory Creation**: Uses Files.createDirectories() which creates parent directories as needed
3. **File Writing**: Uses Files.write() for simple text file creation
4. **ZIP Creation**: Custom zipDirectory() method that recursively zips folder contents
5. **Path Tracking**: Uses a HashMap to track unique locationId paths for zipping

## Customization

To add more sample data, modify the `createSampleData()` method:
```java
dataList.add(new LocationData(year, "agencyId", "companyId", "locationId", "locationName"));
```

## Requirements
- Java 8 or higher (uses java.nio.file and java.util.zip)
- Write permissions in the directory where the program is run
