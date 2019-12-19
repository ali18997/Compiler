

/* *
 * Developed  for the class project in COP5556 Programming Language Principles 
 * at the University of Florida, Fall 2019.
 * 
 * This software is solely for the educational benefit of students 
 * enrolled in the course during the Fall 2019 semester.  
 * 
 * This software, and any software derived from it,  may not be shared with others or posted to public web sites or repositories,
 * either during the course or afterwards.
 * 
 *  @Beverly A. Sanders, 2019
 */
package cop5556fa19;


import static cop5556fa19.Token.Kind.*;


import java.io.IOException;
import java.io.Reader;

public class Scanner {
	
	Reader r;
	
	//State Names
	private enum State {START, EQ, COLON,
		SLASH, XOR, LT, GT, DOT,
		DOTDOT, MINUS}
	
	//Coordinates of characters being read at any time instant
	public int pos=0;  int line=0;
	
	//Character stored for analysis at any time instant
	int ch;

	//Previous Character at any time instant
	int lastCharacter;
	
	//Indicator to use Previous Character or not
	boolean readLastCharacter = false;

	@SuppressWarnings("serial")
	public static class LexicalException extends Exception {	
		public LexicalException(String arg0) {
			super(arg0);
		}
	}
	
	//Constructor
	public Scanner(Reader r) throws IOException {
		this.r = r;
	}
	
	public Scanner(Reader r, boolean b) throws IOException {
		this.r = r;
	}


	public Token getNext() throws Exception {
		
		//If indicator is true, use last character again
		if (readLastCharacter) {
			
			//Current Character is assigned the Last Character again
			ch = lastCharacter;
			
			//reset the indicator back to false
			readLastCharacter = false;
		}
		//Else indicator is false, so read a new character from string
		else{
			
			//Reading a new character
			ch = r.read();
			//And keeping count of it
			pos += 1;
		}
		
		//Token initialized by null value
		Token t = null;
		
		StringBuilder sb;
		
		//Start State is always the first state
		State state = State.START; 
		
		//Unless a token is generated or an error is found, it will keep looping
		while(t==null) {
			
			//Check the current State
			switch(state) {
			
				//START State handled here
				case START: {
					  
					//Check the type of current character
					switch (ch) {
						
						//Whitespace so ignore and get the next character
						case ' ': {   
							ch = r.read();
							pos += 1;
						}
						break;
						
						//Whitespace so ignore and get the next character
						case '\t': {
							ch = r.read();
							pos += 2;
						}
						break;
						
						//Whitespace so ignore and get the next character
						case '\f': {
							ch = r.read();
							pos += 1;
						}
						break;
						
						//Whitespace so ignore and get the next character
						case '\n': {
							ch = r.read();
							line += 1;
							pos = 0;	
						}
						break;
						
						//Whitespace so ignore and get the next character
						case '\r': {
							ch = r.read();
							
							//Usually Return is followed by Newline to represent single change of line and not double
							if(ch == '\n') {
								ch = r.read();
								line += 1;
								pos = 0;		
							}
						}
						break;
						
						//Generate OP_PLUS Token
						case '+': {
							t = new Token(OP_PLUS, "+", pos, line);	
						}
						break;
						
						//Generate OP_TIMES Token
						case '*': {
							t = new Token(OP_TIMES, "*", pos, line);	
						}
						break;
						
						//Generate OP_MOD Token
						case '%': {
							t = new Token(OP_MOD, "%", pos, line);	
						}
						break;
						
						//Generate OP_POW Token
						case '^': {
							t = new Token(OP_POW, "^", pos, line);	
						}
						break;
						
						//Generate OP_HASH Token
						case '#': {
							t = new Token(OP_HASH, "#", pos, line);	
						}
						break;
						
						//Generate BIT_AMP Token
						case '&': {
							t = new Token(BIT_AMP, "&", pos, line);	
						}
						break;
						
						//Generate BIT_OR Token
						case '|': {
							t = new Token(BIT_OR, "|", pos, line);	
						}
						break;
						
						//Generate LPAREN Token
						case '(': {
							t = new Token(LPAREN, "(", pos, line);	
						}
						break;
						
						//Generate RPAREN Token
						case ')': {
							t = new Token(RPAREN, ")", pos, line);	
						}
						break;
						
						//Generate LCURLY Token
						case '{': {
							t = new Token(LCURLY, "{", pos, line);	
						}
						break;
						
						//Generate RCURLY Token
						case '}': {
							t = new Token(RCURLY, "}", pos, line);	
						}
						break;
						
						//Generate LSQUARE Token
						case '[': {
							t = new Token(LSQUARE, "[", pos, line);	
						}
						break;
						
						//Generate RSQUARE Token
						case ']': {
							t = new Token(RSQUARE, "]", pos, line);	
						}
						break;
						
						//Generate SEMI Token
						case ';': {
							t = new Token(SEMI, ";", pos, line);	
						}
						break;
						
						//Generate COMMA Token
						case ',': {
							t = new Token(COMMA, ",", pos, line);
						}
						break;
						
						//Change State to EQ
						case '=': {
							state = State.EQ;
						}
						break;
						
						//Change State to COLON
						case ':': {
							state = State.COLON;
						}
						break;
						
						//Change State to SLASH
						case '/': {
							state = State.SLASH;
						}
						break;
						
						//Change State to XOR
						case '~': {
							state = State.XOR;
						}
						break;
						
						//Change State to LT
						case '<': {
							state = State.LT;
						}
						break;
						
						//Change State to GT
						case '>': {
							state = State.GT;
						}
						break;
						
						//Change State to DOT
						case '.': {
							state = State.DOT;
						}
						break;
						
						//Change State to MINUS
						case '-': {
							state = State.MINUS;
						}
						break;
						
						//If Double Quotes " are detected, start forming a String Literal
						case 34: {
							sb = new StringBuilder();
							
							//Change Current Character from starting quotes to first character
							ch = r.read();
							pos += 1;
							
							//It will keep looping till ending quotes are found or an error occurs
							while(ch != 34) {
								
								//If back slash is detected, check for valid escape sequence
								if(ch == 92){
									
									//Get character after back slash
									ch = r.read();
									pos += 1;
									
									//Check that next character
									switch(ch) {
									
										//Bell character detected
										case 'a':{
											sb.append('\u0007');
										}
										break;
										
										//Backspace character detected
										case 'b':{
											sb.append((char)8);
										}
										break;
										
										//Form feed character detected
										case 'f':{
											sb.append((char)12);
										}
										break;
										
										//Newline character detected
										case 'n':{
											sb.append((char)10);
										}
										break;
										
										//Return character detected
										case 'r':{
											sb.append((char)13);
										}
										break;
										
										//Horizontal Tab character detected
										case 't':{
											sb.append((char)9);
										}
										break;
										
										//Vertical Tab character detected
										case 'v':{
											sb.append((char)11);
										}
										break;
										
										//Escaped Back Slash detected
										case 92:{
											sb.append((char)92);
										}
										break;
										
										//Escaped Double Quotes detected
										case 34:{
											sb.append((char)34);
										}
										break;
										
										//Escaped Single Quote detected
										case 39:{
											sb.append((char)39);
										}
										break;
										
										//Anything other than above is not allowed in grammar so generate an error
										default:{
											throw new LexicalException("Illegal escape sequence in string literal " + (char)92 + (char)ch + " at Line " + (line+1) + ", Position " + pos);
										}
									}	
								}
								
								//Non-escaped Single Quote is not allowed
								else if(ch == 39) {
									throw new LexicalException("Illegal String at Line " + (line+1) + ", Position " + pos+", unescaped \' not allowed in string literal.");
								}
								
								//If EOF is found it means String wasn't closed with end quotes
								else if(ch == -1) {
									throw new LexicalException("No closing \" found");
								}
								
								//Undefined ASCII Value
								else if(ch < 0 || ch > 127) {
									throw new LexicalException("Illegal ASCII value in string literal at Line " + (line+1) + ", Position " + pos+". Value should be 0 to 127.");
								}
								
								//Everything that clears the above tests is a normal value for a string so keep adding it
								else {
									sb.append((char)ch);
								}
								
								//Get the next character for next iteration of loop
								ch = r.read();
								pos += 1;
							}
							
							//String literal complete, generate the token
							t = new Token(STRINGLIT, (char)34 + sb.toString() + (char)34, pos, line);
						}
						break;
						
						//If Double Quotes " are detected, start forming a String Literal
						case 39: {
							sb = new StringBuilder();
							
							//Change Current Character from starting quotes to first character
							ch = r.read();
							pos += 1;
							
							//It will keep looping till ending quote is found or an error occurs
							while(ch != 39) {
								
								//If back slash is detected, check for valid escape sequence
								if(ch == 92){
									
									//Get character after back slash
									ch = r.read();
									pos += 1;
									
									//Check that next character
									switch(ch) {
									
										
										//Bell character detected
										case 'a':{
											sb.append('\u0007');
										}
										break;
										
										//Backspace character detected
										case 'b':{
											sb.append((char)8);
										}
										break;
										
										//Form feed character detected
										case 'f':{
											sb.append((char)12);
										}
										break;
										
										//Newline character detected
										case 'n':{
											sb.append((char)10);
										}
										break;
										
										//Return character detected
										case 'r':{
											sb.append((char)13);
										}
										break;
										
										//Horizontal Tab character detected
										case 't':{
											sb.append((char)9);
										}
										break;
										
										//Vertical Tab character detected
										case 'v':{
											sb.append((char)11);
										}
										break;
										
										//Escaped Back Slash detected
										case 92:{
											sb.append((char)92);
										}
										break;
										
										//Escaped Double Quotes detected
										case 34:{
											sb.append((char)34);
										}
										break;
										
										//Escaped Single Quote detected
										case 39:{
											sb.append((char)39);
										}
										break;
										
										//Anything other than above is not allowed in grammar so generate an error
										default:{
											throw new LexicalException("Illegal escape sequence in string literal " + (char)92 + (char)ch + " at Line " + (line+1) + ", Position " + pos);
										}
									}	
								}
								
								//Non-escaped Double Quotes are not allowed
								else if(ch == 34) {
									throw new LexicalException("Illegal String at Line " + (line+1) + ", Position " + pos+", unescaped \" not allowed in string literal.");
								}
								
								//If EOF is found it means String wasn't closed with end quotes
								else if(ch == -1) {
									throw new LexicalException("No closing \" found");
								}
								
								//Undefined ASCII Value
								else if(ch < 0 || ch > 127) {
									throw new LexicalException("Illegal ASCII value in string literal at Line " + (line+1) + ", Position " + pos+". Value should be 0 to 127.");
								}
								
								//Everything that clears the above tests is a normal value for a string so keep adding it
								else {
									sb.append((char)ch);
								}
								
								//Get the next character for next iteration of loop
								ch = r.read();
								pos += 1;
							}
							
							//String literal complete, generate the token
							t = new Token(STRINGLIT, (char)39 + sb.toString() + (char)39, pos, line);
						}
						break;
						
						//Generate EOF Token
						case -1: {
							t = new Token(EOF, "EOF", pos, line); 
						}
						break;
						
						//The rest need more detailed checks
						default: {
							
							//Digit Detected
							if (Character.isDigit(ch)) {
								if(ch == '0') {
									t = new Token(INTLIT, "0", pos, line);
								}
								else {
									sb = new StringBuilder();
									//Keep appending digits as long as digits keep coming
									while(Character.isDigit(ch)) {
										sb.append((char)ch);
										ch = r.read();
										pos += 1;
									}
									
									//A non digit value was detected which stopped the loop but this value should be used in next iteration
									readLastCharacter = true;
									
									//Check if INTLIT is in range
									try{
										Integer.parseInt(sb.toString());
										
										//Generate INTLIT Token
										t = new Token(INTLIT, sb.toString(), pos-sb.length(), line);
										
									}
									//INTLIT not in range, throw an error
									catch(Exception e) {
										throw new LexicalException("Integer out of range at " + (line +1) + ", Position " + pos);
									}	
								}	
							}
							
							//Check for Identifier of Keywords
							else if (Character.isJavaIdentifierStart(ch)) {                
								sb = new StringBuilder();
								while(Character.isJavaIdentifierStart(ch) || Character.isDigit(ch) || ch == '$' || ch == '_') {
									pos += 1;
									sb.append((char)ch);
									ch = r.read();
								}
								
								readLastCharacter = true;
								
								if(sb.toString().equals("and")) {
									t = new Token(KW_and, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("break")) {
									t = new Token(KW_break, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("do")) {
									t = new Token(KW_do, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("else")) {
									t = new Token(KW_else, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("elseif")) {
									t = new Token(KW_elseif, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("end")) {
									t = new Token(KW_end, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("false")) {
									t = new Token(KW_false, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("for")) {
									t = new Token(KW_for, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("function")) {
									t = new Token(KW_function, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("goto")) {
									t = new Token(KW_goto, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("if")) {
									t = new Token(KW_if, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("in")) {
									t = new Token(KW_in, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("local")) {
									t = new Token(KW_local, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("nil")) {
									t = new Token(KW_nil, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("not")) {
									t = new Token(KW_not, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("or")) {
									t = new Token(KW_or, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("repeat")) {
									t = new Token(KW_repeat, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("return")) {
									t = new Token(KW_return, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("then")) {
									t = new Token(KW_then, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("true")) {
									t = new Token(KW_true, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("until")) {
									t = new Token(KW_until, sb.toString(), pos-sb.length(), line); 
								}
								else if (sb.toString().equals("while")) {
									t = new Token(KW_while, sb.toString(), pos-sb.length(), line); 
								}
								else {
									t = new Token(NAME, sb.toString(), pos-sb.length()+1, line); 
								}
								
								
							}              
							else { 
								throw new LexicalException("Illegal and Unidentifiable Input " + ch +  " at Line " + (line +1) + ", Position " + pos);
								
					
							}          
						}    
					} 
				} 
				break;
				
				//EQ State handled here
				case EQ: {
					ch = r.read();
					if (ch == '=') {
						pos += 1;
						t = new Token(REL_EQEQ, "==", pos, line);
					}
					else {
						t = new Token(ASSIGN, "=", pos, line);
						readLastCharacter = true;
					}	
				} 
				break;
				
				//COLON State handled here
				case COLON: {
					ch = r.read();
					if (ch == ':') {
						pos += 1;
						t = new Token(COLONCOLON, "::", pos, line);
					}
					else {
						t = new Token(COLON, ":", pos, line);
						readLastCharacter = true;
					}	
				} 
				break;
				
				//SLASH State handled here
				case SLASH: {
					ch = r.read();
					if (ch == '/') {
						pos += 1;
						t = new Token(OP_DIVDIV, "//", pos, line);
					}
					else {
						t = new Token(OP_DIV, "/", pos, line);
						readLastCharacter = true;
					}	
				} 
				break;
				
				//XOR State handled here
				case XOR: {
					ch = r.read();
					if (ch == '=') {
						pos += 1;
						t = new Token(REL_NOTEQ, "~=", pos, line);
					}
					else {
						t = new Token(BIT_XOR, "~", pos, line);
						readLastCharacter = true;
					}	
				} 
				break;
				
				//LT State handled here
				case LT: {
					ch = r.read();
					if (ch == '=') {
						pos += 1;
						t = new Token(REL_LE, "<=", pos, line);
					}
					else if(ch == '<') {
						pos += 1;
						t = new Token(BIT_SHIFTL, "<<", pos, line);
					}
					else {
						t = new Token(REL_LT, "<", pos, line);
						readLastCharacter = true;
					}	
				} 
				break;
				
				//GT State handled here
				case GT: {
					ch = r.read();
					if (ch == '=') {
						pos += 1;
						t = new Token(REL_GE, ">=", pos, line);
					}
					else if(ch == '>') {
						pos += 1;
						t = new Token(BIT_SHIFTR, ">>", pos, line);
					}
					else {
						t = new Token(REL_GT, ">", pos, line);
						readLastCharacter = true;
					}	
				} 
				break;
				
				//DOT State handled here
				case DOT: {
					ch = r.read();
					if (ch == '.') {
						pos += 1;
						state = State.DOTDOT;
					}
					else {
						t = new Token(DOT, ".", pos, line);
						readLastCharacter = true;
					}	
				} 
				break;
				
				//DOTDOT State handled here
				case DOTDOT: {
					ch = r.read();
					if (ch == '.') {
						pos += 1;
						t = new Token(DOTDOTDOT, "...", pos, line);
					}
					else {
						t = new Token(DOTDOT, "..", pos, line);
						readLastCharacter = true;
					}	
				} 
				break;
				
				//MINUS State handled here
				case MINUS: {
					ch = r.read();
					if (ch == '-') {
						pos += 1;
						while(ch != 10 && ch != 13 && ch != -1) {
							pos += 1;
							ch = r.read();
						}
						ch = r.read();
						pos += 1;
						state = State.START;
						
					}
					else {
						t = new Token(OP_MINUS, "-", pos, line);
						readLastCharacter = true;
					}	
				} 
				break;
				
				default: throw new LexicalException("Wrong State Reached");
			}
		} 
		lastCharacter = ch;
		
		return t;
		    			
	}

}
