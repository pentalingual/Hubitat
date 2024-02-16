Task Scheduler ~  Create Task

Triggers


Actions

Action 1. 

Program/script:
        C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
Argument:
        Get-Date | Out-File -FilePath .\batteryreport.txt
Location:
        *Select your desired shared network location that Hubitat can access*


Action 2. 

Program/script:
        C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
Argument:
        WMIC PATH Win32_Battery Get EstimatedChargeRemaining | Out-File -FilePath .\batteryreport.txt -Append
Location:
        *Select your desired shared network location that Hubitat can access*
