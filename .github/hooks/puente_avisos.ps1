param([string]$Evento = '', [string]$Mostrar64 = '')

$Segundos         = 9
$ColorFondo       = '#FFE14D'
$ColorTexto       = '#1A1A1A'
$ColorSi          = '#2E7D32'
$ColorNo          = '#A32020'
$ColorDelicado    = '#7B3FA0'
$TextoDelicado    = '#FFFFFF'
$Ancho            = 300
$Alto             = 44
$AnchoBoton       = 42
$LetraTitulo      = 8
$LetraTexto       = 7.5
$LargoTexto       = 70
$SiNoTocas        = $true
$Repetir          = 30
$SegundosDelicado = 10
$AnchoDelicado    = 430
$AltoDelicado     = 80
$LargoLinea       = 62
$Delicado         = '(?i)\b(curl|wget|iwr|irm|Invoke-WebRequest|Invoke-RestMethod|Start-BitsTransfer|Remove-Item|regedit|git\s+push)\b|(^|[;&|(]\s*|"command":")(del|erase|rd|rmdir|reg|format)\s|rm\s+-[a-z]*r|[\\/]Windows[\\/]|Program Files|[\\/]\.ssh|\b[de]:[\\/]'

if ($Mostrar64) {
  $partes = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($Mostrar64)) -split '\|', 2
  Add-Type -ReferencedAssemblies System.Windows.Forms, System.Drawing -TypeDefinition @"
using System;
using System.Drawing;
using System.Windows.Forms;

public class BloqueoForm : Form {
  private Timer anim; private Timer life; private int targetTop;
  public BloqueoForm(string title, string text) {
    int w = 380, h = 88, closeW = 44;
    FormBorderStyle = FormBorderStyle.None; ShowInTaskbar = false; TopMost = true;
    StartPosition = FormStartPosition.Manual;
    BackColor = ColorTranslator.FromHtml("#7B3FA0");
    Width = w; Height = h; Cursor = Cursors.Hand;

    Rectangle area = Screen.PrimaryScreen.WorkingArea;
    Left = area.Right - w - 6; targetTop = area.Bottom - h - 6; Top = area.Bottom;

    int textoW = w - closeW - 18;
    Label l1 = new Label();
    l1.Text = title; l1.ForeColor = Color.White; l1.AutoSize = false;
    l1.Font = new Font("Segoe UI", 10f, FontStyle.Bold);
    l1.SetBounds(10, 8, textoW, 22);

    Label l2 = new Label();
    l2.Text = text; l2.ForeColor = Color.White; l2.AutoSize = false;
    l2.Font = new Font("Segoe UI", 9.5f, FontStyle.Regular);
    l2.SetBounds(10, 32, textoW, h - 38);

    Label x = new Label();
    x.Text = "\u2715"; x.ForeColor = Color.White;
    x.BackColor = ColorTranslator.FromHtml("#A32020");
    x.AutoSize = false; x.TextAlign = ContentAlignment.MiddleCenter;
    x.Font = new Font("Segoe UI Symbol", 15f, FontStyle.Bold);
    x.SetBounds(w - closeW - 2, 2, closeW, h - 4);

    Controls.Add(l1); Controls.Add(l2); Controls.Add(x);

    Paint += delegate(object s, PaintEventArgs e) {
      using (Pen p = new Pen(Color.Black, 2)) e.Graphics.DrawRectangle(p, 1, 1, w - 2, h - 2);
    };

    EventHandler close = delegate { Close(); };
    x.Click += close; Click += close; l1.Click += close; l2.Click += close;

    anim = new Timer(); anim.Interval = 15;
    anim.Tick += delegate { if (Top > targetTop) Top = Math.Max(targetTop, Top - 8); else anim.Stop(); };
    life = new Timer(); life.Interval = 9000; life.Tick += close;
    Shown += delegate { anim.Start(); life.Start(); };
  }
  protected override bool ShowWithoutActivation { get { return true; } }
  protected override CreateParams CreateParams {
    get { CreateParams cp = base.CreateParams; cp.ExStyle |= 0x08 | 0x80 | 0x08000000; return cp; }
  }
}
"@
  [Windows.Forms.Application]::Run((New-Object BloqueoForm -ArgumentList $partes[0], $partes[1]))
  exit 0
}

[Console]::InputEncoding = [Text.Encoding]::UTF8
$raw = ''
if ([Console]::IsInputRedirected) {
  try { $raw = [Console]::In.ReadToEnd() } catch { }
}

if ($Evento -eq 'agentStop' -or $Evento -eq 'sessionEnd') {
  Start-Process powershell.exe -WindowStyle Hidden -ArgumentList @(
    '-NoProfile', '-ExecutionPolicy', 'Bypass', '-WindowStyle', 'Hidden',
    '-File', 'C:\Users\Francis\aviso_terminado.ps1', '-Probar'
  ) | Out-Null
  Write-Output '{}'
  exit 0
}

if ($Evento -eq 'preToolUse') {
  $obj = $null
  try { if ($raw) { $obj = $raw | ConvertFrom-Json } } catch { }

  $herramienta = if ($obj -and $obj.toolName) { [string]$obj.toolName } else { '' }
  $argsObj = $null
  if ($obj -and $obj.toolArgs) {
    if ($obj.toolArgs -is [string]) {
      try { $argsObj = $obj.toolArgs | ConvertFrom-Json } catch { $argsObj = $obj.toolArgs }
    } else {
      $argsObj = $obj.toolArgs
    }
  }

  $todo = if ($argsObj -is [string]) { $argsObj } elseif ($argsObj) { ($argsObj | ConvertTo-Json -Compress -Depth 10) } else { $raw }
  $texto = "$herramienta $todo"
  $textoRutas = ($texto -replace '\\\\+', '\') -replace '/', '\'
  $motivo = $null

  if ($texto -match '(?i)\b(curl|wget|iwr|irm|Invoke-WebRequest|Invoke-RestMethod|Start-BitsTransfer|bitsadmin|certutil|scp|sftp)\b|Net\.WebClient|DownloadFile|DownloadString') {
    $motivo = 'Descarga o transferencia de red prohibida.'
  }
  elseif ($texto -match '(?i)\bgit\s+(push|reset\s+--hard|clean\s+-[a-zA-Z]*f|branch\s+-D)\b') {
    $motivo = 'Comando Git remoto o destructivo prohibido.'
  }
  elseif ($texto -match '(?i)(\brm\s+-[a-zA-Z]*r|\b(del|erase|rd|rmdir)\b[^\r\n]*\s/[sqSQ]|\b(Remove-Item|ri)\b[^\r\n]*-Recurse)') {
    $motivo = 'Borrado masivo o recursivo prohibido.'
  }
  elseif ($texto -match '(?i)\b(reg\s+(add|delete|import)|regedit|diskpart|Format-Volume|shutdown|Restart-Computer|Set-ExecutionPolicy|icacls|takeown|iex|Invoke-Expression)\b|-(EncodedCommand|enc)\b|-Verb\s+RunAs|HKLM:|HKCU:') {
    $motivo = 'Comando de sistema, registro, elevacion o ejecucion ofuscada prohibido.'
  }
  elseif ($textoRutas -match '(?i)(\\\.ssh(\\|$|")|\\\.aws(\\|$|")|\\Windows(\\|$|")|\\Program Files(\s*\(x86\))?(\\|$|")|\\ProgramData(\\|$|")|\\System32(\\|$|")|(^|[^A-Za-z0-9_])[D-Z]:\\|\\Users\\[^\\]+\\(Documents|Desktop|Downloads)(\\|$|"))') {
    $motivo = 'Acceso a ruta protegida del sistema, carpeta personal u otro disco prohibido.'
  }

  if ($motivo) {
    $b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("MetajavBots_next (BLOQUEADO)|$motivo"))
    Start-Process powershell.exe -WindowStyle Hidden -ArgumentList @(
      '-NoProfile', '-ExecutionPolicy', 'Bypass', '-WindowStyle', 'Hidden',
      '-File', "`"$PSCommandPath`"", '-Mostrar64', $b64
    ) | Out-Null
    Write-Output (@{
      permissionDecision       = 'deny'
      permissionDecisionReason = "Bloqueado por politica local de seguridad: $motivo"
    } | ConvertTo-Json -Compress)
    exit 0
  }

  if ($herramienta -notmatch '(?i)(shell|bash|powershell|terminal|run|exec|write|edit|create|replace|delete|patch|file)') {
    Write-Output '{}'
    exit 0
  }
  if ($herramienta -match '(?i)^(read|view|get|list|search|grep|glob|find)') {
    Write-Output '{}'
    exit 0
  }

  Add-Type -ReferencedAssemblies System.Windows.Forms, System.Drawing -TypeDefinition @"
using System;
using System.Drawing;
using System.Windows.Forms;

public class PermisoForm : Form {
  public string Respuesta = "";
  private Timer anim; private Timer life; private int targetTop;
  public PermisoForm(string title, string text, Color back, Color fore, Color si, Color no,
                     int w, int h, int bw, int seconds, float ft, float fx) {
    FormBorderStyle = FormBorderStyle.None; ShowInTaskbar = false; TopMost = true;
    StartPosition = FormStartPosition.Manual; BackColor = back; Width = w; Height = h;
    Rectangle area = Screen.PrimaryScreen.WorkingArea;
    Left = area.Right - w - 3; targetTop = area.Bottom - h - 3; Top = area.Bottom;
    int textoW = w - 2 * bw - 6;
    int altoTitulo = (int)(ft * 2) + 3;
    Label l1 = new Label(); l1.Text = title; l1.ForeColor = fore; l1.AutoSize = false; l1.AutoEllipsis = true;
    l1.Font = new Font("Segoe UI", ft, FontStyle.Bold); l1.SetBounds(4, 1, textoW, altoTitulo);
    Label l2 = new Label(); l2.Text = text; l2.ForeColor = fore; l2.AutoSize = false; l2.AutoEllipsis = true;
    l2.Font = new Font("Segoe UI", fx); l2.SetBounds(4, altoTitulo, textoW, h - altoTitulo - 1);
    Label bSi = Boton("S\u00ed", si, w - 2 * bw, bw, h);
    Label bNo = Boton("No", no, w - bw, bw, h);
    bSi.Click += delegate { Respuesta = "allow"; Close(); };
    bNo.Click += delegate { Respuesta = "deny"; Close(); };
    Controls.Add(l1); Controls.Add(l2); Controls.Add(bSi); Controls.Add(bNo);
    anim = new Timer(); anim.Interval = 15;
    anim.Tick += delegate { if (Top > targetTop) Top = Math.Max(targetTop, Top - 6); else anim.Stop(); };
    life = new Timer(); life.Interval = seconds * 1000; life.Tick += delegate { Close(); };
    Shown += delegate { anim.Start(); life.Start(); };
  }
  static Label Boton(string t, Color c, int x, int bw, int h) {
    Label b = new Label(); b.Text = t; b.ForeColor = Color.White; b.BackColor = c; b.AutoSize = false;
    b.TextAlign = ContentAlignment.MiddleCenter; b.Cursor = Cursors.Hand;
    b.Font = new Font("Segoe UI", 9f, FontStyle.Bold); b.SetBounds(x, 0, bw, h);
    b.MouseEnter += delegate { b.BackColor = ControlPaint.Dark(c, 0.2f); };
    b.MouseLeave += delegate { b.BackColor = c; };
    return b;
  }
  protected override bool ShowWithoutActivation { get { return true; } }
  protected override CreateParams CreateParams {
    get { CreateParams cp = base.CreateParams; cp.ExStyle |= 0x08 | 0x80 | 0x08000000; return cp; }
  }
}

public class DelicadoForm : Form {
  public string Respuesta = "";
  private Timer anim; private Timer life; private int targetTop;
  public DelicadoForm(string title, string que, string porque, string riesgo, Color back, Color fore,
                      Color si, Color no, int w, int h, int bw, int seconds) {
    FormBorderStyle = FormBorderStyle.None; ShowInTaskbar = false; TopMost = true;
    StartPosition = FormStartPosition.Manual; BackColor = back; Width = w; Height = h;
    Rectangle area = Screen.PrimaryScreen.WorkingArea;
    Left = area.Right - w - 3; targetTop = area.Bottom - h - 3; Top = area.Bottom;
    int textoW = w - 2 * bw - 6;
    Label t = new Label(); t.Text = title; t.ForeColor = fore; t.AutoSize = false; t.AutoEllipsis = true;
    t.Font = new Font("Segoe UI", 9f, FontStyle.Bold); t.SetBounds(5, 2, textoW, 19);
    Controls.Add(t);
    string[] etiquetas = { "Qu\u00e9:", "Por qu\u00e9:", "Riesgo:" };
    string[] textos = { que, porque, riesgo };
    int fila = 19, y = 21, anchoEtiqueta = 64;
    for (int i = 0; i < 3; i++) {
      Label e = new Label(); e.Text = etiquetas[i]; e.ForeColor = fore; e.AutoSize = false;
      e.Font = new Font("Segoe UI", 9f, FontStyle.Bold); e.SetBounds(5, y + i * fila, anchoEtiqueta, fila);
      Label v = new Label(); v.Text = textos[i]; v.ForeColor = fore; v.AutoSize = false; v.AutoEllipsis = true;
      v.Font = new Font("Segoe UI", 9f); v.SetBounds(5 + anchoEtiqueta, y + i * fila, textoW - anchoEtiqueta - 1, fila);
      Controls.Add(e); Controls.Add(v);
    }
    Label bSi = PermisoBoton("S\u00ed", si, w - 2 * bw, bw, h);
    Label bNo = PermisoBoton("No", no, w - bw, bw, h);
    bSi.Click += delegate { Respuesta = "allow"; Close(); };
    bNo.Click += delegate { Respuesta = "deny"; Close(); };
    Controls.Add(bSi); Controls.Add(bNo);
    anim = new Timer(); anim.Interval = 15;
    anim.Tick += delegate { if (Top > targetTop) Top = Math.Max(targetTop, Top - 6); else anim.Stop(); };
    life = new Timer(); life.Interval = seconds * 1000; life.Tick += delegate { Close(); };
    Shown += delegate { anim.Start(); life.Start(); };
  }
  static Label PermisoBoton(string t, Color c, int x, int bw, int h) {
    Label b = new Label(); b.Text = t; b.ForeColor = Color.White; b.BackColor = c; b.AutoSize = false;
    b.TextAlign = ContentAlignment.MiddleCenter; b.Cursor = Cursors.Hand;
    b.Font = new Font("Segoe UI", 10f, FontStyle.Bold); b.SetBounds(x, 0, bw, h);
    b.MouseEnter += delegate { b.BackColor = ControlPaint.Dark(c, 0.2f); };
    b.MouseLeave += delegate { b.BackColor = c; };
    return b;
  }
  protected override bool ShowWithoutActivation { get { return true; } }
  protected override CreateParams CreateParams {
    get { CreateParams cp = base.CreateParams; cp.ExStyle |= 0x08 | 0x80 | 0x08000000; return cp; }
  }
}
"@

  function Get-Color([string]$h) { return [Drawing.ColorTranslator]::FromHtml($h) }
  function Corta([string]$t) {
    $t = ($t -replace '\s+', ' ').Trim()
    if ($t.Length -gt $LargoLinea) { $t = $t.Substring(0, $LargoLinea - 3) + '...' }
    return $t
  }

  $que = ''
  if ($argsObj -and -not ($argsObj -is [string])) {
    foreach ($campo in 'command', 'cmd', 'file_path', 'path', 'url', 'pattern', 'query', 'prompt') {
      if ($argsObj.PSObject.Properties[$campo] -and $argsObj.$campo) {
        $que = [string]$argsObj.$campo
        break
      }
    }
  }
  if (-not $que) { $que = if ($argsObj -is [string] -and $argsObj) { $argsObj } else { $herramienta } }
  $que = ($que -replace '\s+', ' ').Trim()
  if ($que.Length -gt $LargoTexto) { $que = $que.Substring(0, $LargoTexto - 3) + '...' }

  $delicado = ($todo -match $Delicado)
  $pista = if ($delicado) { ' (DELICADO: Si o No)' } elseif ($SiNoTocas) { ' (si no tocas: Si)' } else { ' (si no tocas: pregunta)' }

  if ($delicado) {
    $dQue = Corta $que
    $dPorque = '(operacion delicada solicitada por Copilot)'
    $dRiesgo = 'Revisa antes de aprobar'
    $f = New-Object DelicadoForm -ArgumentList "MetajavBots_next - $herramienta$pista", $dQue, $dPorque, $dRiesgo,
      (Get-Color $ColorDelicado), (Get-Color $TextoDelicado), (Get-Color $ColorSi), (Get-Color $ColorNo),
      $AnchoDelicado, $AltoDelicado, ([int]($AnchoBoton * 4 / 3)), $SegundosDelicado
    [Windows.Forms.Application]::Run($f)
    $r = $f.Respuesta
    while ($r -eq '') {
      Start-Sleep -Seconds $Repetir
      $f = New-Object DelicadoForm -ArgumentList "MetajavBots_next - $herramienta$pista", $dQue, $dPorque, $dRiesgo,
        (Get-Color $ColorDelicado), (Get-Color $TextoDelicado), (Get-Color $ColorSi), (Get-Color $ColorNo),
        $AnchoDelicado, $AltoDelicado, ([int]($AnchoBoton * 4 / 3)), $SegundosDelicado
      [Windows.Forms.Application]::Run($f)
      $r = $f.Respuesta
    }
  } else {
    $f = New-Object PermisoForm -ArgumentList "MetajavBots_next - $herramienta$pista", $que,
      (Get-Color $ColorFondo), (Get-Color $ColorTexto), (Get-Color $ColorSi), (Get-Color $ColorNo),
      $Ancho, $Alto, $AnchoBoton, $Segundos, ([single]$LetraTitulo), ([single]$LetraTexto)
    [Windows.Forms.Application]::Run($f)
    $r = $f.Respuesta
  }

  if ($r -eq '' -and $SiNoTocas -and -not $delicado) { $r = 'allow' }

  if ($r -eq 'allow') {
    Write-Output '{"permissionDecision":"allow"}'
  } elseif ($r -eq 'deny') {
    Write-Output '{"permissionDecision":"deny","permissionDecisionReason":"Fran lo ha denegado desde el aviso."}'
  } else {
    Write-Output '{}'
  }
  exit 0
}

Write-Output '{}'
exit 0
