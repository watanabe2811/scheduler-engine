// $Id: spooler_http.h 13029 2007-09-25 09:10:07Z jz $

#ifndef __SPOOLER_HTTP_H
#define __SPOOLER_HTTP_H


namespace sos {
namespace scheduler {
namespace http {

//--------------------------------------------------------------------------------------------const

const int                       recommended_chunk_size = 32768;

//-------------------------------------------------------------------------------------------------

struct Operation_connection;
struct Operation;
struct Request;
struct C_request;
struct Response;

//-------------------------------------------------------------------------------------------------

string                          get_content_type_parameter  ( const string& content_type, const string& param_name );
string                          date_string                 ( time_t );

//-------------------------------------------------------------------------------------Chunk_reader
/*
    Verwendung:

    while(1)
    {
        while( !next_chunk_is_ready() )  wait();

        int size = get_next_chunk_size();
        if( size == 0 )  break;  // EOF
        read_from_chunk()  bis genau size Bytes gelesen
    }
*/

struct Chunk_reader : Object, javabridge::has_proxy<Chunk_reader>
{
                                Chunk_reader                ( const string& content_type )          : _zero_(this+1), _recommended_block_size( recommended_chunk_size ), _content_type(content_type) {}

    string                      content_type                () const                                { return _content_type; }

    virtual void                recommend_block_size        ( int size )                            { _recommended_block_size = size; }
    int                         recommended_block_size      () const                                { return _recommended_block_size; }
    virtual void            set_event                       ( Event_base* )                         {}


    virtual bool                next_chunk_is_ready         ()                                      = 0;
    virtual int                 get_next_chunk_size         ()                                      = 0;
    virtual string              read_from_chunk             ( int size )                            = 0;
    virtual string              html_insertion              ()                                      { return ""; }      // Irgendeine HTML-Einfügung, z.B. <hr/>


    Fill_zero                  _zero_;
    int                        _recommended_block_size;
    string                     _content_type;
};

//-----------------------------------------------------------------------------String_chunk_reader

struct String_chunk_reader : Chunk_reader
{
                                String_chunk_reader         ( const string& text, const string& content_type = "text/plain; charset=" + string_encoding ) 
                                                                                                    : Chunk_reader( content_type ), _zero_(this+1), _text(text) {}

    bool                        next_chunk_is_ready         ()                                      { return true; }
    int                         get_next_chunk_size         ();
    string                      read_from_chunk             ( int size );

  private:
    Fill_zero                  _zero_;
    string                     _text;
   //bool                       _get_next_chunk_size_called;
    uint                       _offset;                     // Bereits gelesene Bytes
};

//-------------------------------------------------------------------------------Byte_chunk_reader

struct Byte_chunk_reader : String_chunk_reader
{
                                Byte_chunk_reader           ( const Byte* bytes, size_t length, const string& content_type ) 
                                                                                                    : String_chunk_reader( string( (const char*)bytes, length ), content_type ) {}
};

//-------------------------------------------------------------------------------Byte_chunk_reader
/*
struct Byte_chunk_reader : String_chunk_reader
{
                                Byte_chunk_reader           ( const Byte* bytes, size_t length, const Charset& charset ) 
                                                                                                    : String_chunk_reader( string( (const char*)bytes, length ), conent_type ) {}
};
*/
//-----------------------------------------------------------------------------String_chunk_reader
/*
struct Olechar_chunk_reader : Chunk_reader
{
                                Olechar_chunk_reader        ( const OLECHAR* text, size_t length, const string& content_type = "text/plain; charset=UTF-8" ) 
                                                                                                    : Chunk_reader( content_type ), _zero_(this+1), _text(text) {}

  protected:
    bool                        next_chunk_is_ready         ()                                      { return true; }
    int                         get_next_chunk_size         ();
    string                      read_from_chunk             ( int size );


    Fill_zero                  _zero_;
    Bstr                       _text;
    bool                       _get_next_chunk_size_called;
    uint                       _offset;                     // Bereits gelesene Bytes
};
*/
//--------------------------------------------------------------------------------Log_chunk_reader

struct Log_chunk_reader : Chunk_reader
{
                                Log_chunk_reader            ( Prefix_log* );
                               ~Log_chunk_reader            ();

    void                    set_event                       ( Event_base* );


  protected:
    bool                        next_chunk_is_ready         ();
    int                         get_next_chunk_size         ();
    string                      read_from_chunk             ( int size );
    virtual string              html_insertion              ();


    Fill_zero                  _zero_;
    ptr<Prefix_log>            _log;
    Event_base*                _event;

    enum State { s_initial, s_reading_string, s_reading_file, s_finished };
    State                      _state;
    ptr<String_chunk_reader>   _string_chunk_reader;
    size_t                     _file_seek;
    File                       _file;
    int                        _log_instance_number;
    string                     _html_insertion;
};

//------------------------------------------------------------------------------Chunk_reader_filter

struct Chunk_reader_filter : Chunk_reader
{
                                Chunk_reader_filter         ( Chunk_reader* r, const string& content_type ) : Chunk_reader( content_type ), _chunk_reader(r) {}

    void                        set_event                   ( Event_base* event )                   { _chunk_reader->set_event( event ); }

    ptr<Chunk_reader>          _chunk_reader;
};

//--------------------------------------------------------------------------------Html_chunk_reader
// Konvertiert Text nach HTML

struct Html_chunk_reader : Chunk_reader_filter
{
    enum State { reading_prefix, reading_text, reading_suffix, reading_finished };


                                Html_chunk_reader           ( Chunk_reader*, const string& url_base, const string& title );
                               ~Html_chunk_reader           ();

    virtual void                recommend_block_size        ( int size )                            { _chunk_reader->recommend_block_size( size );
                                                                                                      Chunk_reader_filter::recommend_block_size( size ); }

  protected:
    bool                        next_chunk_is_ready         ();
    int                         get_next_chunk_size         ();
    string                      read_from_chunk             ( int size );
    bool                        try_fill_chunk              ();
    bool                        try_fill_line               ();


    Fill_zero                  _zero_;
    State                      _state;
    string                     _html_prefix;
    string                     _html_suffix;
    int                        _available_net_chunk_size;
    string                     _chunk;
    bool                       _chunk_filled;

    int                        _in_span;                    // Wir müssen am Zeilenende soviele </span> schreiben
    bool                       _at_begin_of_line;

    string                     _line;                       
    bool                       _is_begin_of_line;
    string                     _html_insertion;
    string                     _next_characters;
    size_t                     _next_offset;
};

//--------------------------------------------------------------------------------------Status_code

enum Status_code
{
    status_200_ok                           = 200,
    status_301_moved_permanently            = 301,
    status_401_permission_denied            = 401,
    status_403_forbidden                    = 403,
    status_404_bad_request                  = 404,
    status_405_method_not_allowed           = 405,
    status_500_internal_server_error        = 500,
    status_501_not_implemented              = 501,
    status_504_gateway_timeout              = 504,
    status_505_http_version_not_supported   = 505
};

//-------------------------------------------------------------------------------------------------

struct Http_exception : exception
{
                                Http_exception              ( Status_code, const string& error_text = "" );
                               ~Http_exception              () throw ();


    const char*                 what                        () const throw()                        { return _what.c_str(); }
    Status_code                _status_code;
    string                     _error_text;
    string                     _what;
};

//-------------------------------------------------------------------------------------------------

extern stdext::hash_map<int,string>  http_status_messages;

//-------------------------------------------------------------------------------------------------

struct Headers
{
    struct Entry
    {
                                Entry                       ()                                          {}
                                Entry                       ( const string& name, const string& value ) : _name(name), _value(value) {}

        string                 _name;
        string                 _value;
    };

    typedef stdext::hash_map<string,Entry>   Map;


    bool                        contains                    ( const string& name )                  { return _map.find( lcase( name ) ) != _map.end(); }
    string                      operator[]                  ( const string& name ) const            { return get( name ); }
    string                      get                         ( const string& name ) const;
    void                        set                         ( const string& name, const string& value );
    void                        set_unchecked               ( const string& name, const string& value );
    void                        set_default                 ( const string& name, const string& value );
    void                        print                       ( String_stream* ) const;
    void                        print                       ( String_stream*, const Map::const_iterator& ) const;
    void                        print                       ( String_stream*, const string& header_name ) const;
    void                        print_and_remove            ( String_stream*, const string& header_name );
    void                    set_content_type_parameter      ( const string& name, const string& value );
    void                    set_charset_name                ( const string& value )                 { set_content_type_parameter( "charset", value ); }
    string                      charset_name                () const                                { return get_content_type_parameter( get( "Content-Type" ), "charset" ); }
    void                    set_content_type                ( const string& );
    string                      content_type                () const;

    STDMETHODIMP            put_Header                      ( BSTR name, BSTR value );
    STDMETHODIMP            get_Header                      ( BSTR name, BSTR* result );
    STDMETHODIMP            get_Content_type                ( BSTR* result );
    STDMETHODIMP            get_Charset_name                ( BSTR* result );
    STDMETHODIMP            put_Content_type                ( BSTR );
    STDMETHODIMP            put_Charset_name                ( BSTR );

    Map                        _map;
};

//--------------------------------------------------------------------------------------------Parser

struct Parser : Object
{
                                Parser                      ( C_request* );

    void                        close                       ();
    void                        add_text                    ( const char*, int len );
    bool                        is_complete                 ();

    void                        parse_header                ();
    void                        eat_spaces                  ();
    void                        eat                         ( const char* str );
    string                      eat_word                    ();
    string                      eat_until                   ( const char* character_set );
    string                      eat_path                    ();
    void                        eat_line_end                ();
    char                        next_char                   ()                                      { return *_next_char; }
    const string&               text                        () const                                { return _text; }               // Zum Debuggen

  private:
    Fill_zero                  _zero_;
    string                     _text;
    bool                       _reading_body;
    int                        _body_start;
    int                        _content_length;
    const char*                _next_char;
    C_request*                 _request;
};

//------------------------------------------------------------------------------------------Request

struct Request : Object {
    virtual bool                has_parameter               ( const string& name ) = 0;
    virtual string              parameter                   ( const string& name ) = 0;
    virtual string              header                      ( const string& name ) = 0;
    virtual string              protocol                    () = 0;
    virtual bool                is_http_1_1                 () { return protocol() == "HTTP/1.1"; }
    virtual string              url_path                    () = 0;
    virtual string              charset_name                () = 0;
    virtual string              http_method                 () = 0;
    virtual string              body                        () = 0;
};

//----------------------------------------------------------------------------------------C_request

struct C_request : Request
{
                                C_request                   ()                                      : _zero_(this+1){}

  //void                        close                       ();
    void                        check                       ();
    bool                        has_parameter               ( const string& name )                  { return _parameters.find( name ) != _parameters.end(); }
    string                      parameter                   ( const string& name );      
    string                      protocol                    ()                                      { return _protocol; }
    string                      header                      ( const string& name )                  { return _headers[ name ]; }
    string                      url                         ();        
    string                      url_path                    ()                                      { return _path; }
    string                      charset_name                ();        
    virtual string              http_method                 ()                                      { return _http_method; }
    string                      body                        ()                                      { return _body; }

    STDMETHODIMP            get_Url                         ( BSTR* result )                        { return String_to_bstr( url(), result ); }
    STDMETHODIMP            get_Header                      ( BSTR name, BSTR* result )             { return String_to_bstr( header( string_from_bstr( name ) ), result ); }
    STDMETHODIMP            get_Content_type                ( BSTR* result )                        { return _headers.get_Content_type( result ); }
    STDMETHODIMP            get_Charset_name                ( BSTR* result )                        { return _headers.get_Charset_name( result ); }
    STDMETHODIMP            get_Binary_content              ( SAFEARRAY** result );
    STDMETHODIMP            get_String_content              ( BSTR* result );


  //private:
    friend struct               Parser;
    friend struct               Operation;
    friend struct               Response;

    Fill_zero                  _zero_;
    string                     _http_method;
    string                     _protocol;                                                      
    string                     _path;

    Headers                    _headers;
    typedef stdext::hash_map<string,string>  String_map;
    String_map                 _parameters;
    string                     _body;
};


//-----------------------------------------------------------------------------------------Response

struct Response : Object
{
                                Response                    (Request*);
                               ~Response                    ();

    void                        recommend_block_size        ( int size )                            { if( _chunk_reader )  _chunk_reader->recommend_block_size( size ); }
    int                         recommended_block_size      () const                                { return _chunk_reader? _chunk_reader->_recommended_block_size : recommended_chunk_size; }
    Chunk_reader*               chunk_reader                () const                                { return _chunk_reader; }

    bool                        close_connection_at_eof     ()                                      { return _close_connection_at_eof; }
    virtual bool                closed                      () = 0;
    void                    set_event                       ( Event_base* event )                   { if( _chunk_reader )  _chunk_reader->set_event( event ); }

    int                         status                      () const                                { return (int)_status_code; }
    void                    set_header                      ( const string& name, const string& value );
    string                      header                      ( const string& name )                  { return _headers[ name ]; }
    string                      header_string               () const                                { S s; _headers.print(&s); return s; }
    void                    set_status                      ( Status_code, const string& text = "" );
    void                    set_chunk_reader                ( Chunk_reader* );
    void                        finish                      ();
    void                        send                        ();
    bool                     is_ready                       () const                                { return _ready; }
    virtual void            set_ready                       ();
    virtual void                on_ready                    () = 0;

    bool                        eof                         ();
    string                      read                        ( int recommended_size );

    STDMETHODIMP                Assert_is_not_ready         ();
    STDMETHODIMP            put_Status_code                 ( int code );
    STDMETHODIMP            put_Header                      ( BSTR name, BSTR value );
    STDMETHODIMP            get_Header                      ( BSTR name, BSTR* result )             { return _headers.get_Header( name, result ); }
    STDMETHODIMP            put_Content_type                ( BSTR );
    STDMETHODIMP            get_Content_type                ( BSTR* result )                        { return _headers.get_Content_type( result ); }
    STDMETHODIMP            put_Charset_name                ( BSTR );
    STDMETHODIMP            get_Charset_name                ( BSTR* result )                        { return _headers.get_Charset_name( result ); }
    STDMETHODIMP            put_String_content              ( BSTR );
    STDMETHODIMP            put_Binary_content              ( SAFEARRAY* );
    STDMETHODIMP                Send                        ();

  protected:
    string                      start_new_chunk             ();

    Fill_zero                  _zero_;
    string const               _protocol;
    Headers                    _headers;
    bool const                 _chunked;
    bool                       _close_connection_at_eof;
    Status_code                _status_code;
    ptr<Chunk_reader>          _chunk_reader;
    bool                       _ready;                      // Antwort kann versendet werden
    bool                       _finished;                   // read() ist vorbereitet, eigentlich dasselbe wie _ready
    String_stream              _headers_stream;
    int                        _chunk_index;                // 0: Header
    uint                       _chunk_size;
    uint                       _chunk_offset;               // Bereits gelesene Bytes
    bool                       _chunk_eof;
    bool                       _eof;
};

//---------------------------------------------------------------------------------------C_response

struct C_response : Response {
    private: Fill_zero _zero_;
    private: Operation* const _operation;

    public:                 C_response                      (Operation*);
    bool                    closed                          () { return _operation == NULL; }
    void                    on_ready                        ();
};

//---------------------------------------------------------------------------------new_java_request

ptr<Request> new_java_request(const SchedulerHttpRequestJ&);

//------------------------------------------------------------------------------------Java_response

struct Java_response : Response, Signalable, javabridge::has_proxy<Java_response> {
    private: SchedulerHttpResponseJ _responseJ;
    private: Callback_event _event;
    private: bool _closed;

    public: Java_response(Request*, const SchedulerHttpResponseJ&);
    public: ~Java_response();
    public: void close();
    public: bool closed();
    public: void on_ready();
    public: void on_event_signaled();
};

//-----------------------------------------------------------------------------------Operation

struct Operation : Communication::Operation
{
                                Operation                   ( Operation_connection* );

    void                        close                       ();
    bool                        closed                      ()                                      { return _response == NULL; }
    xml::Element_ptr            dom_element                 ( const xml::Document_ptr&, const Show_what& ) const;

    void                        put_request_part            ( const char* data, int length )        { _parser->add_text( data, length ); }
    bool                        request_is_complete         ()                                      { return !_parser  ||  _parser->is_complete(); }

    void                        begin                       ();
    void                        cancel                      ();
    virtual bool                async_continue_             ( Continue_flags );
    virtual bool                async_finished_             () const                                { return _response  &&  _response->is_ready(); }
    virtual string              async_state_text_           () const;

    void                        link_order                  ( Order* );                             // Für Web_service_operation::begin()
    void                        unlink_order                ();                                     // Für Order::close()
    void                        on_first_order_processing   ( Task* );

    bool                        response_is_complete        ();
    string                      get_response_part           ();
    bool                        should_close_connection     ();
    Web_service_operation*      web_service_operation_or_null()                                     { return _web_service_operation; }

    C_request*                  request                     () const                                { return _request; }
    Response*                   response                    () const                                { return _response; }

    string                      obj_name                    () const                                { return "http::Operation"; }

  private:
    friend struct               C_request;
    friend struct               Response;

    Fill_zero                  _zero_;
    ptr<C_request>             _request;
    ptr<Parser>                _parser;
    ptr<Response>            _response;
    ptr<Web_service_operation> _web_service_operation;
    Order*                     _order;
};

//-----------------------------------------------------------------------------Operation_connection

struct Operation_connection : Communication::Operation_connection
{
                                Operation_connection        ( Communication::Connection* c )        : Communication::Operation_connection( c ) {}

    ptr<Communication::Operation> new_operation             ()                                      { ptr<Operation> result = Z_NEW( Operation( this ) ); 
                                                                                                      return +result; }

    string                      connection_type             () const                                { return "HTTP"; }
};

//----------------------------------------------------------------------------Http_server_interface

struct Http_server_interface: Object, Subsystem
{
                                Http_server_interface       ( Scheduler* scheduler, Type_code t )  : Subsystem( scheduler, this, t ) {}

    virtual File_path           directory                   () const                                = 0;
};


ptr<Http_server_interface>      new_http_server             ( Scheduler* );

//-------------------------------------------------------------------------------------------------

} //namespace http
} //namespace scheduler
} //namespace sos

#endif
