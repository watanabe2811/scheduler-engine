// *** Generated by com.sos.scheduler.engine.cplusplus.generator ***

#ifndef _JAVAPROXY_ORG_W3C_DOM_ELEMENT_H_
#define _JAVAPROXY_ORG_W3C_DOM_ELEMENT_H_

#include "../zschimmer/zschimmer.h"
#include "../zschimmer/java.h"
#include "../zschimmer/Has_proxy.h"
#include "../zschimmer/javaproxy.h"
#include "../zschimmer/lazy.h"
#include "java__lang__Object.h"

namespace javaproxy { namespace java { namespace lang { struct Object; }}}
namespace javaproxy { namespace java { namespace lang { struct String; }}}
namespace javaproxy { namespace org { namespace w3c { namespace dom { struct Attr; }}}}
namespace javaproxy { namespace org { namespace w3c { namespace dom { struct Node; }}}}
namespace javaproxy { namespace org { namespace w3c { namespace dom { struct NodeList; }}}}


namespace javaproxy { namespace org { namespace w3c { namespace dom { 


struct Element__class;

struct Element : ::zschimmer::javabridge::proxy_jobject< Element >, ::javaproxy::java::lang::Object {
  private:
    static Element new_instance();  // Not implemented
  public:

    Element(jobject = NULL);

    Element(const Element&);

    #ifdef Z_HAS_MOVE_CONSTRUCTOR
        Element(Element&&);
    #endif

    ~Element();

    Element& operator=(jobject jo) { assign_(jo); return *this; }
    Element& operator=(const Element& o) { assign_(o.get_jobject()); return *this; }
    #ifdef Z_HAS_MOVE_CONSTRUCTOR
        Element& operator=(Element&& o) { set_jobject(o.get_jobject()); o.set_jobject(NULL); return *this; }
    #endif

    jobject get_jobject() const { return ::zschimmer::javabridge::proxy_jobject< Element >::get_jobject(); }

  protected:
    void set_jobject(jobject jo) {
        ::zschimmer::javabridge::proxy_jobject< Element >::set_jobject(jo);
        ::javaproxy::java::lang::Object::set_jobject(jo);
    }
  public:

    ::javaproxy::java::lang::String getAttribute(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0) const;
    ::javaproxy::java::lang::String getAttributeNS(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0, const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p1) const;
    ::javaproxy::org::w3c::dom::Attr getAttributeNode(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0) const;
    ::javaproxy::org::w3c::dom::Attr getAttributeNodeNS(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0, const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p1) const;
    ::javaproxy::org::w3c::dom::NodeList getElementsByTagName(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0) const;
    ::javaproxy::org::w3c::dom::NodeList getElementsByTagNameNS(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0, const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p1) const;
    ::javaproxy::java::lang::String getTagName() const;
    bool hasAttribute(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0) const;
    bool hasAttributeNS(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0, const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p1) const;
    void removeAttribute(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0) const;
    void removeAttributeNS(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0, const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p1) const;
    ::javaproxy::org::w3c::dom::Attr removeAttributeNode(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::org::w3c::dom::Attr >& p0) const;
    void setAttribute(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0, const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p1) const;
    void setAttributeNS(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0, const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p1, const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p2) const;
    ::javaproxy::org::w3c::dom::Attr setAttributeNode(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::org::w3c::dom::Attr >& p0) const;
    ::javaproxy::org::w3c::dom::Attr setAttributeNodeNS(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::org::w3c::dom::Attr >& p0) const;
    void setIdAttribute(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0, jboolean p1) const;
    void setIdAttributeNS(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p0, const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::java::lang::String >& p1, jboolean p2) const;
    void setIdAttributeNode(const ::zschimmer::javabridge::proxy_jobject< ::javaproxy::org::w3c::dom::Attr >& p0, jboolean p1) const;

    ::zschimmer::javabridge::Class* java_object_class_() const;

    static ::zschimmer::javabridge::Class* java_class_();


  private:
    struct Lazy_class : ::zschimmer::abstract_lazy<Element__class*> {
        void initialize() const;
    };

    Lazy_class _class;
};


}}}}

#endif