// *** Generated by com.sos.scheduler.engine.cplusplus.generator ***

#ifndef _JAVAPROXY_COM_SOS_SCHEDULER_ENGINE_KERNEL_ORDER_JOBCHAIN_NODE_H_
#define _JAVAPROXY_COM_SOS_SCHEDULER_ENGINE_KERNEL_ORDER_JOBCHAIN_NODE_H_

#include "../zschimmer/zschimmer.h"
#include "../zschimmer/java.h"
#include "../zschimmer/Has_proxy.h"
#include "../zschimmer/javaproxy.h"
#include "../zschimmer/lazy.h"
#include "java__lang__Object.h"

namespace javaproxy { namespace com { namespace sos { namespace scheduler { namespace engine { namespace cplusplus { namespace runtime { struct Sister; }}}}}}}
namespace javaproxy { namespace java { namespace lang { struct Object; }}}
namespace javaproxy { namespace java { namespace lang { struct String; }}}


namespace javaproxy { namespace com { namespace sos { namespace scheduler { namespace engine { namespace kernel { namespace order { namespace jobchain { 


struct Node__class;

struct Node : ::zschimmer::javabridge::proxy_jobject< Node >, ::javaproxy::java::lang::Object {
  private:
    static Node new_instance();  // Not implemented
  public:

    Node(jobject = NULL);

    Node(const Node&);

    #ifdef Z_HAS_MOVE_CONSTRUCTOR
        Node(Node&&);
    #endif

    ~Node();

    Node& operator=(jobject jo) { assign_(jo); return *this; }
    Node& operator=(const Node& o) { assign_(o.get_jobject()); return *this; }
    #ifdef Z_HAS_MOVE_CONSTRUCTOR
        Node& operator=(Node&& o) { set_jobject(o.get_jobject()); o.set_jobject(NULL); return *this; }
    #endif

    jobject get_jobject() const { return ::zschimmer::javabridge::proxy_jobject< Node >::get_jobject(); }

  protected:
    void set_jobject(jobject jo) {
        ::zschimmer::javabridge::proxy_jobject< Node >::set_jobject(jo);
        ::javaproxy::java::lang::Object::set_jobject(jo);
    }
  public:


    ::zschimmer::javabridge::Class* java_object_class_() const;

    static ::zschimmer::javabridge::Class* java_class_();


  private:
    struct Lazy_class : ::zschimmer::abstract_lazy<Node__class*> {
        void initialize() const;
    };

    Lazy_class _class;
};


}}}}}}}}

#endif