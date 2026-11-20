param([switch]$Probar)

# ===== Lo que puedes cambiar =====
$Segundos         = 9          # tiempo que se queda el cartel normal
$ColorFondo       = '#FFE14D'  # amarillo
$ColorTexto       = '#1A1A1A'
$ColorSi          = '#2E7D32'  # verde
$ColorNo          = '#A32020'  # rojo
$ColorDelicado    = '#7B3FA0'  # violeta (permisos delicados)
$TextoDelicado    = '#FFFFFF'  # texto blanco en lo delicado
$Ancho            = 300
$Alto             = 44
$AnchoBoton       = 42
$LetraTitulo      = 8
$LetraTexto       = 7.5
$LargoTexto       = 70
$SiNoTocas        = $true      # $false = si no tocas nada, pregunta en la terminal
$Repetir          = 30         # segundos entre repeticiones en lo delicado
$SegundosDelicado = 10
$AnchoDelicado    = 430
$AltoDelicado     = 80
$LargoLinea       = 62
$Delicado         = '(?i)\b(curl|wget|iwr|irm|Invoke-WebRequest|Invoke-RestMethod|Start-BitsTransfer|Remove-Item|regedit|git\s+push)\b|(^|[;&|(]\s*|"command":")(del|erase|rd|rmdir|reg|format)\s|rm\s+-[a-z]*r|[\\/]Windows[\\/]|Program Files|[\\/]\.ssh|\b[de]:[\\/]'
# =================================

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

function Show-Permiso([string]$titulo, [string]$texto) {
  $f = New-Object PermisoForm -ArgumentList $titulo, $texto,
    (Get-Color $ColorFondo), (Get-Color $ColorTexto), (Get-Color $ColorSi), (Get-Color $ColorNo),
    $Ancho, $Alto, $AnchoBoton, $Segundos, ([single]$LetraTitulo), ([single]$LetraTexto)
  [Windows.Forms.Application]::Run($f)
  return $f.Respuesta
}

function Show-Delicado([string]$titulo, [string]$que, [string]$porque, [string]$riesgo) {
  $f = New-Object DelicadoForm -ArgumentList $titulo, $que, $porque, $riesgo,
    (Get-Color $ColorDelicado), (Get-Color $TextoDelicado), (Get-Color $ColorSi), (Get-Color $ColorNo),
    $AnchoDelicado, $AltoDelicado, ([int]($AnchoBoton * 4 / 3)), $SegundosDelicado
  [Windows.Forms.Application]::Run($f)
  return $f.Respuesta
}

function Corta([string]$t) {
  $t = ($t -replace '\s+', ' ').Trim()
  if ($t.Length -gt $LargoLinea) { $t = $t.Substring(0, $LargoLinea - 3) + '...' }
  return $t
}

if ($Probar) {
  $r = Show-Delicado 'MetajavBots_next - shell (DELICADO: Si o No)' 'Descargar dependencia externa' 'Necesaria para compilar el modulo' 'Codigo externo no verificado'
  Write-Host "Delicado: '$r' (vacia = no tocaste nada)"
  $r = Show-Permiso 'MetajavBots_next - shell (si no tocas: Si)' './gradlew test (ejemplo)'
  Write-Host "Normal: '$r' (vacia = no tocaste nada)"
  exit 0
}

# Leer JSON de Copilot CLI
[Console]::InputEncoding = [Text.Encoding]::UTF8
$raw = ''
if ([Console]::IsInputRedirected) {
  try { $raw = [Console]::In.ReadToEnd() } catch { exit 0 }
}
if (-not $raw) { exit 0 }

$datos = $null
try { $datos = $raw | ConvertFrom-Json } catch { exit 0 }
if (-not $datos) { exit 0 }

$proyecto = 'MetajavBots_next'
if ($datos.cwd) { $proyecto = Split-Path $datos.cwd -Leaf }

$herramienta = if ($datos.toolName) { [string]$datos.toolName } elseif ($datos.tool_name) { [string]$datos.tool_name } else { 'tool' }

$argsObj = $null
if ($datos.toolArgs) {
  if ($datos.toolArgs -is [string]) {
    try { $argsObj = $datos.toolArgs | ConvertFrom-Json } catch { $argsObj = $datos.toolArgs }
  } else {
    $argsObj = $datos.toolArgs
  }
} elseif ($datos.tool_input) {
  $argsObj = $datos.tool_input
}

$todo = if ($argsObj -is [string]) { $argsObj } elseif ($argsObj) { ($argsObj | ConvertTo-Json -Compress -Depth 6) } else { $raw }
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
  $desc = ''
  if ($argsObj -and -not ($argsObj -is [string]) -and $argsObj.PSObject.Properties['description']) {
    $desc = [string]$argsObj.description
  }
  $dQue = ''; $dPorque = ''; $dRiesgo = ''
  if ($desc) {
    $m = [regex]::Match($desc, '(?is)(?<!por\s)qu[e\u00e9]\s*:\s*(.*?)\s*(\||por\s+qu[e\u00e9]\s*:|riesgo\s*:|$)')
    if ($m.Success) { $dQue = $m.Groups[1].Value }
    $m = [regex]::Match($desc, '(?is)por\s+qu[e\u00e9]\s*:\s*(.*?)\s*(\||riesgo\s*:|$)')
    if ($m.Success) { $dPorque = $m.Groups[1].Value }
    $m = [regex]::Match($desc, '(?is)riesgo\s*:\s*(.*?)\s*(\||$)')
    if ($m.Success) { $dRiesgo = $m.Groups[1].Value }
    if (-not ($dQue -or $dPorque -or $dRiesgo)) { $dPorque = $desc }
  }
  if (-not $dQue)    { $dQue = $que }
  if (-not $dPorque) { $dPorque = '(sin explicacion del modelo)' }
  if (-not $dRiesgo) { $dRiesgo = 'Operacion delicada: revisa antes de aprobar' }

  $dQue = Corta $dQue; $dPorque = Corta $dPorque; $dRiesgo = Corta $dRiesgo
  $r = Show-Delicado "$proyecto - $herramienta$pista" $dQue $dPorque $dRiesgo
  while ($r -eq '') {
    Start-Sleep -Seconds $Repetir
    $r = Show-Delicado "$proyecto - $herramienta$pista" $dQue $dPorque $dRiesgo
  }
} else {
  $r = Show-Permiso "$proyecto - $herramienta$pista" $que
}

if ($r -eq '' -and $SiNoTocas -and -not $delicado) { $r = 'allow' }

if ($r -eq 'allow') {
  @{ permissionDecision = 'allow' } | ConvertTo-Json -Compress
} elseif ($r -eq 'deny') {
  @{
    permissionDecision       = 'deny'
    permissionDecisionReason = 'Fran lo ha denegado desde el aviso.'
  } | ConvertTo-Json -Compress
}
exit 0
