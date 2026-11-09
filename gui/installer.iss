; Windows 安装程序（Inno Setup 6）。由 GitHub Actions 调用：iscc /DAPPVER=0.1.0 gui\installer.iss
#ifndef APPVER
  #define APPVER "0.0.0"
#endif
[Setup]
AppId={{9B651DD2-1ED7-4858-8144-D7DB4C623510}
AppName=Ai.Clean
AppVersion={#APPVER}
AppPublisher=Ai.Clean
AppPublisherURL=https://github.com/Tri250/AlcedoStudio
DefaultDirName={autopf}\QingjingAiGoodBro
DefaultGroupName=Ai.Clean
DisableProgramGroupPage=yes
OutputDir=dist
OutputBaseFilename=AiClean-{#APPVER}-windows-setup
SetupIconFile=icon.ico
UninstallDisplayIcon={app}\AndroidAdCleaner.exe
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
#if FileExists(AddBackslash(SourcePath) + "ChineseSimplified.isl")
Name: "zh"; MessagesFile: "ChineseSimplified.isl"
#else
Name: "en"; MessagesFile: "compiler:Default.isl"
#endif

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[InstallDelete]
; 0.3.2 及更早按旧产品名建了开始菜单组与桌面快捷方式；装 Ai.Clean 前装过上游「安卓去广告」的机器上，
; 那两组快捷方式不和本安装器同键，不会按升级清掉，不删就和新的并存。
; 公共级那两条只在提权安装时删：以前可能按「所有用户」装过，而这次选了按用户装，没权限删公共目录会让安装报错。
Type: filesandordirs; Name: "{userprograms}\安卓去广告"
Type: filesandordirs; Name: "{commonprograms}\安卓去广告"; Check: IsAdminInstallMode
Type: files; Name: "{autodesktop}\安卓去广告.lnk"
Type: files; Name: "{commondesktop}\安卓去广告.lnk"; Check: IsAdminInstallMode

[Files]
Source: "dist\AndroidAdCleaner\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Ai.Clean"; Filename: "{app}\AndroidAdCleaner.exe"
Name: "{autodesktop}\Ai.Clean"; Filename: "{app}\AndroidAdCleaner.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\AndroidAdCleaner.exe"; Description: "{cm:LaunchProgram,Ai.Clean}"; Flags: nowait postinstall skipifsilent
