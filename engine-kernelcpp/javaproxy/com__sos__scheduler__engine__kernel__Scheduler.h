// *** Generated by com.sos.scheduler.engine.cplusplus.generator ***

#ifndef _JAVAPROXY_COM_SOS_SCHEDULER_ENGINE_KERNEL_SCHEDULER_H_
#define _JAVAPROXY_COM_SOS_SCHEDULER_ENGINE_KERNEL_SCHEDULER_H_

#include "../zschimmer/zschimmer.h"
#include "../zschimmer/java.h"
#include "../zschimmer/Has_proxy.h"
#include "../zschimmer/javaproxy.h"
#include "../zschimmer/lazy.h"
#include "java__lang__Object.h"

namespace javaproxy { namespace com { namespace google { namespace inject { struct Injector; }}}}
namespace javaproxy { namespace com { namespace sos { namespace scheduler { namespace engine { namespace cplusplus { namespace runtime { struct Sister; }}}}}}}
namespace javaproxy { namespace com { namespace sos { namespace scheduler { namespace engine { namespace kernel { namespace async { struct CppCall; }}}}}}}
namespace javaproxy { namespace com { namespace sos { namespace scheduler { namespace engine { namespace kernel { namespace cppproxy { struct SpoolerC; }}}}}}}
namespace javaproxy { namespace com { namespace sos { namespace scheduler { namespace engine { namespace kernel { namespace event { struct EventSubsystem; }}}}}}}
namespace javaproxy { namespace com { namespace sos { namespace scheduler { namespace engine { namespace main { struct SchedulerControllerBridge; }}}}}}
namespace javaproxy { namespace java { namespace lang { struct Object; }}}
namespace javaproxy { namespace java { namespace lang { struct String; }}}


namespace javaproxy { namespace com { namespace sos { namespace scheduler { namespace engine { namespace kernel { 


struct Scheduler__class;

struct Scheduler : ::zschimmer::javabridge::proxy_jobject< Scheduler >, ::javaproxy::java::lang::Object {
  private:
    static Scheduler new_instance();  // Not implemented
  public:

    Scheduler(jobject = NULL);

    Scheduler(const Scheduler&);

    #ifdef Z_HAS_MOVE_CONSTRUCTOR
        Scheduler(Scheduler&&);
    #endif

    ~Scheduler();

    Scheduler& operator=(jobject jo) { assign_(jo); return *this; }
    Scheduler& operator=(const Scheduler& o) { assign_(o.get_jobject()); return *this; }
    #ifdef Z_HAS_MOVE_CONSTRUCTOR
        Scheduler& operator=(Scheduler&& o) { set_jobject(o.get_jobject()); o.set_jobject(NULL); return *this; }
    #endif

    jobject get_jobject() const { return ::zschimmer::javabridge::proxy_jobject< Scheduler >::get_jobject(); }

  protected:
    void set_jobject(jobject jo) {
        ::zschimmer::javabridge::proxy_jobject< Scheduler >::set_jobject(jo);
        ::javaproxy::java::lang::Object::set_jobject(jo);
    }
  public:

    static ::javaproxy::java::lang::String buildVersion();
    void cancelCall(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::com::sos::scheduler::engine::kernel::async::CppCall >& p0) const;
    static ::javaproxy::java::lang::String defaultTimezoneId();
    void enqueueCall(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::com::sos::scheduler::engine::kernel::async::CppCall >& p0) const;
    ::javaproxy::com::sos::scheduler::engine::kernel::event::EventSubsystem getEventSubsystem() const;
    void initialize() const;
    ::javaproxy::java::lang::String javaExecuteXml(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0) const;
    void log(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0, jint p1, const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p2) const;
    static ::javaproxy::com::google::inject::Injector newInjector(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::com::sos::scheduler::engine::kernel::cppproxy::SpoolerC >& p0, const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::com::sos::scheduler::engine::main::SchedulerControllerBridge >& p1, const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p2);
    void onActivate() const;
    void onActivated() const;
    void onClose() const;
    jlong onEnteringSleepState() const;
    void onLoad() const;
    void sendCommandAndReplyToStout(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0, const ::zschimmer::javabridge::Local_java_byte_array& p1) const;
    void threadLock() const;
    void threadUnlock() const;
    static ::javaproxy::java::lang::String versionCommitHash();

    ::zschimmer::javabridge::Class* java_object_class_() const;

    static ::zschimmer::javabridge::Class* java_class_();


  private:
    struct Lazy_class : ::zschimmer::abstract_lazy<Scheduler__class*> {
        void initialize() const;
    };

    Lazy_class _class;
};


}}}}}}

#endif