Task Scheduler ~  Create Task

<strong>Triggers</strong>


<strong>Actions</strong>

<strong>Action 1.</strong> 

Program/script:
        C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
Argument:
        Get-Date | Out-File -FilePath .\batteryreport.txt
Location:
        *Select your desired shared network location that Hubitat can access*


<strong>Action 2. </strong>

Program/script:
        C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
Argument:
        WMIC PATH Win32_Battery Get EstimatedChargeRemaining | Out-File -FilePath .\batteryreport.txt -Append
Location:
        *Select your desired shared network location that Hubitat can access*
