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

[Files]
Source: "dist\AndroidAdCleaner\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Ai.Clean"; Filename: "{app}\AndroidAdCleaner.exe"
Name: "{autodesktop}\Ai.Clean"; Filename: "{app}\AndroidAdCleaner.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\AndroidAdCleaner.exe"; Description: "{cm:LaunchProgram,Ai.Clean}"; Flags: nowait postinstall skipifsilent
