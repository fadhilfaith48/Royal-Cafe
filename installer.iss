[Setup]
AppName=Royal Cafe POS System
AppVersion=1.0
AppPublisher=Fadhil
DefaultDirName={autopf}\RoyalCafe
DefaultGroupName=Royal Cafe
OutputDir=C:\RoyalCafe_dist
OutputBaseFilename=RoyalCafe_Setup
Compression=lzma
SolidCompression=yes
WizardStyle=modern

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Buat shortcut di Desktop"; GroupDescription: "Shortcut:"; Flags: unchecked

[Files]
Source: "C:\RoyalCafe_dist\RoyalCafe.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\RoyalCafe_dist\fadhil.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\RoyalCafe_dist\lib\*"; DestDir: "{app}\lib"; Flags: ignoreversion recursesubdirs
Source: "C:\Users\user\OneDrive\Documents\Cafe Management Shop\Royal_cofe.sql"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\user\OneDrive\Documents\Cafe Management Shop\dist\CARA_INSTALL.txt"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\Royal Cafe"; Filename: "{app}\RoyalCafe.exe"
Name: "{group}\Uninstall Royal Cafe"; Filename: "{uninstallexe}"
Name: "{autodesktop}\Royal Cafe"; Filename: "{app}\RoyalCafe.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\RoyalCafe.exe"; Description: "Jalankan Royal Cafe sekarang"; Flags: nowait postinstall skipifsilent
