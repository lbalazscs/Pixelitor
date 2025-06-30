!include "FileAssociation.nsh"

; HM NIS Edit Wizard helper defines
!define PIXELITOR_NAME "Pixelitor"
!define PIXELITOR_VERSION "4.3.2"
!define PIXELITOR_PUBLISHER "Laszlo Balazs-Csiki"
!define PIXELITOR_WEB_SITE "https://pixelitor.sourceforge.io"
!define PIXELITOR_DIR_REGKEY "Software\Microsoft\Windows\CurrentVersion\App Paths\pixelitor.exe"
!define PIXELITOR_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PIXELITOR_NAME}"
!define PIXELITOR_UNINST_ROOT_KEY "HKLM"

; MUI 1.67 compatible ------
!include "MUI.nsh"

; For the estimated uninstall size
!include "FileFunc.nsh"

; MUI Settings
!define MUI_ABORTWARNING
!define MUI_ICON "pixelitor_icon.ico"
!define MUI_UNICON "pixelitor_icon.ico"


!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "C:\dev_p\pixelitor\installer\MUI_HEADERIMAGE_BITMAP.bmp"
!define MUI_WELCOMEFINISHPAGE_BITMAP "C:\dev_p\pixelitor\installer\MUI_WELCOMEFINISHPAGE_BITMAP.bmp"

; Welcome page
!insertmacro MUI_PAGE_WELCOME
; License page
!insertmacro MUI_PAGE_LICENSE "gplv3_license.txt"
; Directory page
!insertmacro MUI_PAGE_DIRECTORY
; Instfiles page
!insertmacro MUI_PAGE_INSTFILES
; Finish page
!define MUI_FINISHPAGE_LINK "Visit the ${PIXELITOR_NAME} site for the latest version."
!define MUI_FINISHPAGE_LINK_LOCATION "${PIXELITOR_WEB_SITE}"

!define MUI_FINISHPAGE_SHOWREADME ""
!define MUI_FINISHPAGE_SHOWREADME_NOTCHECKED
!define MUI_FINISHPAGE_SHOWREADME_TEXT "Create Desktop Shortcut"
!define MUI_FINISHPAGE_SHOWREADME_FUNCTION finishpageaction

!define MUI_FINISHPAGE_RUN "$INSTDIR\pixelitor.exe"
!insertmacro MUI_PAGE_FINISH

; Uninstaller pages
!insertmacro MUI_UNPAGE_INSTFILES

; Language files
!insertmacro MUI_LANGUAGE "English"

; MUI end ------

Name "${PIXELITOR_NAME} ${PIXELITOR_VERSION}"
OutFile "${PIXELITOR_NAME}_${PIXELITOR_VERSION}_Setup.exe"
InstallDir "$PROGRAMFILES\Pixelitor"
InstallDirRegKey HKLM "${PIXELITOR_DIR_REGKEY}" ""
ShowInstDetails show
ShowUnInstDetails show

VIProductVersion "${PIXELITOR_VERSION}.0"
VIAddVersionKey ProductName "Pixelitor"
VIAddVersionKey ProductVersion ${PIXELITOR_VERSION}
VIAddVersionKey LegalCopyright "GPLv3"
VIAddVersionKey FileDescription "Pixelitor Installer"
VIAddVersionKey FileVersion "${PIXELITOR_VERSION}.0"

Section "MainSection" SEC01
  ; delete old files
  RMDir /r "$INSTDIR\*.*"

  SetOutPath "$INSTDIR"
  File "pixelitor.exe"
  
  SetOutPath "$INSTDIR\runtime"
  File /r "runtime\*.*"
  
  CreateDirectory "$SMPROGRAMS\Pixelitor"
  CreateShortCut "$SMPROGRAMS\Pixelitor\Pixelitor.lnk" "$INSTDIR\pixelitor.exe"
  
  ;Set File Association
  ${registerExtension} "$INSTDIR\pixelitor.exe" ".pxc" "Pixelitor File"
SectionEnd

Section -AdditionalIcons
  WriteIniStr "$INSTDIR\Website.url" "InternetShortcut" "URL" "${PIXELITOR_WEB_SITE}"
  CreateShortCut "$SMPROGRAMS\Pixelitor\Pixelitor Website.lnk" "$INSTDIR\Website.url"
;  CreateShortCut "$SMPROGRAMS\Pixelitor\Uninstall Pixelitor.lnk" "$INSTDIR\pixelitor_uninstaller.exe"
SectionEnd

Section -Post
  WriteUninstaller "$INSTDIR\pixelitor_uninstaller.exe"
  WriteRegStr HKLM "${PIXELITOR_DIR_REGKEY}" "" "$INSTDIR\pixelitor.exe"
  WriteRegStr ${PIXELITOR_UNINST_ROOT_KEY} "${PIXELITOR_UNINST_KEY}" "DisplayName" "$(^Name)"
  WriteRegStr ${PIXELITOR_UNINST_ROOT_KEY} "${PIXELITOR_UNINST_KEY}" "UninstallString" "$INSTDIR\pixelitor_uninstaller.exe"
  WriteRegStr ${PIXELITOR_UNINST_ROOT_KEY} "${PIXELITOR_UNINST_KEY}" "DisplayIcon" "$INSTDIR\pixelitor.exe"
  WriteRegStr ${PIXELITOR_UNINST_ROOT_KEY} "${PIXELITOR_UNINST_KEY}" "DisplayVersion" "${PIXELITOR_VERSION}"
  WriteRegStr ${PIXELITOR_UNINST_ROOT_KEY} "${PIXELITOR_UNINST_KEY}" "URLInfoAbout" "${PIXELITOR_WEB_SITE}"
  WriteRegStr ${PIXELITOR_UNINST_ROOT_KEY} "${PIXELITOR_UNINST_KEY}" "Publisher" "${PIXELITOR_PUBLISHER}"
  
  ; Estimates size - http://nsis.sourceforge.net/Add_uninstall_information_to_Add/Remove_Programs#Computing_EstimatedSize
  ${GetSize} "$INSTDIR" "/S=0K" $0 $1 $2
  IntFmt $0 "0x%08X" $0
  WriteRegDWORD ${PIXELITOR_UNINST_ROOT_KEY} "${PIXELITOR_UNINST_KEY}" "EstimatedSize" "$0"
SectionEnd

Function finishpageaction
  CreateShortcut "$DESKTOP\Pixelitor.lnk" "$INSTDIR\pixelitor.exe"
FunctionEnd

Function un.onUninstSuccess
  HideWindow
  MessageBox MB_ICONINFORMATION|MB_OK "$(^Name) was successfully removed from your computer."
FunctionEnd

Function un.onInit
  MessageBox MB_ICONQUESTION|MB_YESNO|MB_DEFBUTTON2 "Are you sure you want to completely remove $(^Name) and all of its components?" IDYES +2
  Abort
FunctionEnd

Section Uninstall
  ${unregisterExtension} ".pxc" "Pixelitor File"

  RMDir /r "$INSTDIR\*.*"
  RMDir "$INSTDIR"
  
  Delete "$SMPROGRAMS\Pixelitor\Uninstall Pixelitor.lnk"
  Delete "$SMPROGRAMS\Pixelitor\Pixelitor Website.lnk"
  Delete "$DESKTOP\Pixelitor.lnk"
  Delete "$SMPROGRAMS\Pixelitor\Pixelitor.lnk"
  RMDir "$SMPROGRAMS\Pixelitor"
  
  ; Remove user settings
  DeleteRegKey HKEY_CURRENT_USER "Software\JavaSoft\Prefs\pixelitor"

  DeleteRegKey ${PIXELITOR_UNINST_ROOT_KEY} "${PIXELITOR_UNINST_KEY}"
  DeleteRegKey HKLM "${PIXELITOR_DIR_REGKEY}"
  SetAutoClose true
SectionEnd