// $Id: spooler_module_internal.cxx 13855 2010-04-29 15:36:16Z jz $        Joacim Zschimmer, Zschimmer GmbH, http://www.zschimmer.com

#include "spooler.h"

namespace sos {
namespace scheduler {

//-----------------------------------------------------------------Internal_module::Internal_module
    
Internal_module::Internal_module( Spooler* spooler, Prefix_log* log )
:
    Module( spooler, (File_based*)NULL, "::NO-INCLUDE-PATH::", log )
{
    _kind = kind_internal;
    _set = true;
}

//---------------------------------------------------Internal_instance_base::Internal_module_instance
    
Internal_module_instance::Internal_module_instance( Module* module )             
: 
    Module_instance(module),
    _zero_(this+1)
{
}

//----------------------------------------------------------------Internal_module_instance::add_obj
    
void Internal_module_instance::add_obj( IDispatch* idispatch, const string& name )
{
    if( name == "spooler_task" )
    {
        ptr<Com_task> com_task = dynamic_cast<Com_task*>( idispatch );
        _task = com_task->task();
    }
    else
    if( name == "spooler_log" )
    {
        ptr<Com_log> com_log = dynamic_cast<Com_log*>( idispatch );
        _log = com_log->log();
    }
}

//-------------------------------------------------------------------Internal_module_instance::call

Variant Internal_module_instance::call( const string& name )
{
    if( name == spooler_process_name )  return spooler_process();
    return false;
}

//-------------------------------------------------------------------Internal_module_instance::call

Variant Internal_module_instance::call( const string&, const Variant&, const Variant& )
{
    assert(0), z::throw_xc( Z_FUNCTION );
}

//------------------------------------------------------------Internal_module_instance::name_exists

bool Internal_module_instance::name_exists( const string& name )
{
    if( name == spooler_process_name )  return true;

    return false;
}

//-------------------------------------------------------------------------------------------------

} //namespace scheduler
} //namespace sos
