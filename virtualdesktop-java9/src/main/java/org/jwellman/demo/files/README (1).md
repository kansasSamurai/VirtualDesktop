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

### System Temp Directory
The program automatically detects and uses the system's temporary directory:
- **Windows**: Typically `C:\Users\{username}\AppData\Local\Temp`
- **Linux/Mac**: Typically `/tmp`
- Creates a `root` folder within this temp directory

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
- ZIP files are created in the root directory: `{temp}/root/{key}.zip`
- The key format is: `{year}_{agencyId}_{companyId}_{locationId}`
- Example: `{temp}/root/2024_AG001_CO001_LOC001.zip`

## Expected Output

### Console Output
```
Starting File Organizer POC...
Using temp directory: /tmp
Root path: /tmp/root

Processing: LocationData{year=2024, agencyId='AG001', companyId='CO001', locationId='LOC001', locationName='Downtown_Office'}
  Created directory: /tmp/root/2024/abc/AG001/CO001/LOC001
  Created file: /tmp/root/2024/abc/AG001/CO001/LOC001/Downtown_Office_2024.txt

Processing: LocationData{year=2024, agencyId='AG001', companyId='CO002', locationId='LOC002', locationName='Westside_Branch'}
  Created directory: /tmp/root/2024/abc/AG001/CO002/LOC002
  Created file: /tmp/root/2024/abc/AG001/CO002/LOC002/Westside_Branch_2024.txt

Processing: LocationData{year=2023, agencyId='AG002', companyId='CO001', locationId='LOC003', locationName='North_Campus'}
  Created directory: /tmp/root/2023/abc/AG002/CO001/LOC003
  Created file: /tmp/root/2023/abc/AG002/CO001/LOC003/North_Campus_2023.txt

Processing: LocationData{year=2023, agencyId='AG002', companyId='CO003', locationId='LOC004', locationName='East_Facility'}
  Created directory: /tmp/root/2023/abc/AG002/CO003/LOC004
  Created file: /tmp/root/2023/abc/AG002/CO003/LOC004/East_Facility_2023.txt


=== Creating ZIP files ===

Created ZIP: /tmp/root/2024_AG001_CO001_LOC001.zip
Created ZIP: /tmp/root/2024_AG001_CO002_LOC002.zip
Created ZIP: /tmp/root/2023_AG002_CO001_LOC003.zip
Created ZIP: /tmp/root/2023_AG002_CO003_LOC004.zip

=== POC Completed Successfully ===
```

Note: The actual temp directory path will vary based on your operating system.

### File Structure Created
```
{system temp directory}/
└── root/
    ├── 2024_AG001_CO001_LOC001.zip
    ├── 2024_AG001_CO002_LOC002.zip
    ├── 2023_AG002_CO001_LOC003.zip
    ├── 2023_AG002_CO003_LOC004.zip
    ├── 2023/
    │   └── abc/
    │       └── AG002/
    │           ├── CO001/
    │           │   └── LOC003/
    │           │       └── North_Campus_2023.txt
    │           └── CO003/
    │               └── LOC004/
    │                   └── East_Facility_2023.txt
    └── 2024/
        └── abc/
            └── AG001/
                ├── CO001/
                │   └── LOC001/
                │       └── Downtown_Office_2024.txt
                └── CO002/
                    └── LOC002/
                        └── Westside_Branch_2024.txt
```

Where `{system temp directory}` is:
- Windows: `C:\Users\{username}\AppData\Local\Temp`
- Linux: `/tmp`
- Mac: `/var/folders/...` or as defined by TMPDIR

## Key Features Demonstrated

1. **System Temp Directory**: Uses System.getProperty("java.io.tmpdir") to get the OS-specific temp directory
2. **Dynamic Path Construction**: Uses String.format() to build paths with variable values
3. **Directory Creation**: Uses Files.createDirectories() which creates parent directories as needed
4. **File Writing**: Uses Files.write() for simple text file creation
5. **ZIP Creation**: Custom zipDirectory() method that recursively zips folder contents
6. **Path Tracking**: Uses a HashMap to track unique locationId paths for zipping

## Customization

To add more sample data, modify the `createSampleData()` method:
```java
dataList.add(new LocationData(year, "agencyId", "companyId", "locationId", "locationName"));
```

## Requirements
- Java 8 or higher (uses java.nio.file and java.util.zip)
- Write permissions in the system temp directory (typically available by default)
