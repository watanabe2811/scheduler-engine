// *** Generated by com.sos.scheduler.engine.cplusplus.generator ***

#include "_precompiled.h"

#include "com__sos__scheduler__engine__kernel__cppproxy__FolderC.h"
#include "java__lang__Object.h"
#include "java__lang__String.h"

namespace javaproxy { namespace com { namespace sos { namespace scheduler { namespace engine { namespace kernel { namespace cppproxy { 

struct FolderC__class : ::zschimmer::javabridge::Class
{
    FolderC__class(const string& class_name);
   ~FolderC__class();


    static const ::zschimmer::javabridge::class_factory< FolderC__class > class_factory;
};

const ::zschimmer::javabridge::class_factory< FolderC__class > FolderC__class::class_factory ("com.sos.scheduler.engine.kernel.cppproxy.FolderC");

FolderC__class::FolderC__class(const string& class_name) :
    ::zschimmer::javabridge::Class(class_name)
{}

FolderC__class::~FolderC__class() {}




FolderC::FolderC(jobject jo) { if (jo) assign_(jo); }

FolderC::FolderC(const FolderC& o) { assign_(o.get_jobject()); }

#ifdef Z_HAS_MOVE_CONSTRUCTOR
    FolderC::FolderC(FolderC&& o) { set_jobject(o.get_jobject());  o.set_jobject(NULL); }
#endif

FolderC::~FolderC() { assign_(NULL); }





::zschimmer::javabridge::Class* FolderC::java_object_class_() const { return _class.get(); }

::zschimmer::javabridge::Class* FolderC::java_class_() { return FolderC__class::class_factory.clas(); }


void FolderC::Lazy_class::initialize() const {
    _value = FolderC__class::class_factory.clas();
}


}}}}}}}