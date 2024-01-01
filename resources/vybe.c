#include "flecs/flecs.c"

#define VYBE_DEBUG
#ifdef _VYBE_DEBUG

void DEBUGG(
    const char *s) {

    FILE *fp;
    fp = fopen("/tmp/flecs_debug.txt", "a+");
    fprintf(fp, "message: %s\n", s);
    fclose(fp);
}

char str[1024];

#define DDD(...)\
    sprintf(str, __VA_ARGS__);\
    DEBUGG(str)

#endif

// Wrapper for ecs_query_iter so we can return a pointer, JNR does not work
// with returned values.
ecs_iter_t* vybe_query_iter(
    const ecs_world_t *world,
    ecs_query_t *query)
{
    ecs_iter_t iter = ecs_query_iter(world, query);
    // DDD("%s", "\n\n");
    // DDD("iter size: %lu", sizeof(iter));
    // DDD("iter count: %i", iter.table_count);

    ecs_iter_t *iter_ptr = malloc(sizeof(iter));
    memcpy(iter_ptr, &iter, sizeof(iter));
    // DDD("iter_ptr size: %lu", sizeof(*iter_ptr));
    // DDD("iter_ptr count: %i", iter_ptr->table_count);

    return iter_ptr;
}

typedef struct eita_t
{
    float x;
} eita_t;


void vybe_test(eita_t *eita)
{
   DDD("%f", eita->x);
   DDD("size: %lu", sizeof(eita));
}

// Wrapper for ecs_filter_iter so we can return a pointer.
ecs_iter_t* vybe_filter_iter(
    const ecs_world_t *world,
    ecs_filter_t *filter)
{
    ecs_iter_t iter = ecs_filter_iter(world, filter);

    ecs_iter_t *iter_ptr = malloc(sizeof(iter));
    memcpy(iter_ptr, &iter, sizeof(iter));

    return iter_ptr;
}

void vybe_enable_rest(ecs_world_t *world)
{
    ecs_singleton_set(world, EcsRest, {0});
}

// Pair.
ecs_entity_t vybe_pair(ecs_entity_t e1, ecs_entity_t e2)
{
    return (ECS_PAIR | ecs_entity_t_comb(e2, e1));
}

char* vybe_pair_str(ecs_entity_t e1, ecs_entity_t e2)
{
    char *str = malloc(256);
    sprintf(str, "%llu", vybe_pair(e1, e2));
    return str;
}

ecs_entity_t vybe_pair_first(const ecs_world_t *world, ecs_entity_t pair)
{
    return ecs_get_alive(world, ECS_PAIR_FIRST(pair));
}

ecs_entity_t vybe_pair_second(const ecs_world_t *world, ecs_entity_t pair)
{
    return ecs_get_alive(world, ECS_PAIR_SECOND(pair));
}

ecs_entity_t vybe_new_tag(ecs_world_t *world) {
    ecs_entity_t ent = ecs_new_id(world);
    ecs_component_init(world, &(ecs_component_desc_t){.entity = ent, .type = { .alignment = 0, .size = 0}});

    ecs_add_id(world, ent, EcsTag);
    return ent;
}

// Constants.
ecs_entity_t vybe_wildcard()
{
    return EcsWildcard;
}

ecs_entity_t vybe_any()
{
    return EcsAny;
}

ecs_entity_t vybe_ECS_OVERRIDE()
{
    return ECS_OVERRIDE;
}

//////// ECS TEST

typedef struct {
    double x, y;
} Position;

void Observer(ecs_iter_t *it) {
    ecs_world_t *ecs = it->world;
    
    // The event kind
    ecs_entity_t event = it->event;

    // The (component) id for which the event was emitted
    ecs_entity_t event_id = it->event_id;



    // TESTS BEGIN...
    vybe_new_tag(ecs);
    vybe_new_tag(ecs);
    vybe_new_tag(ecs);
    vybe_new_tag(ecs);
    // ...TESTS END





    for (int i = 0; i < it->count; i ++) {
        ecs_entity_t e = it->entities[i];

        DDD(" - %s: %s: %s\n", 
            ecs_get_name(ecs, event), 
            ecs_get_name(ecs, event_id),
            ecs_get_name(ecs, e));
    }
}

ecs_entity_t MyEvent;
ECS_COMPONENT_DECLARE(Position);

void Move(ecs_iter_t *it) {
    Position *p = ecs_field(it, Position, 1);
    ecs_world_t *ecs = it->world;

    for (int i = 0; i < it->count; i ++) {
        ecs_emit(ecs, &(ecs_event_desc_t) {
            .event = MyEvent,
            .ids = &(ecs_type_t){ (ecs_id_t[]){ ecs_id(Position) }, 1 }, // 1 id
            .entity = it->entities[i]
        });
    }
}

void vybe_main() {
    ecs_world_t *ecs = ecs_init();

    ECS_COMPONENT_DEFINE(ecs, Position);

    // Create custom event
    MyEvent = ecs_new_entity(ecs, "MyEvent");

    // Create observer for custom event
    ECS_OBSERVER(ecs, Observer, MyEvent, Position);

    // Create entity
    ecs_entity_t e = ecs_new_entity(ecs, "e");
    
    // The observer filter can be matched against the entity, so make sure it
    // has the Position component before emitting the event. This does not 
    // trigger the observer yet.
    ecs_set(ecs, e, Position, {10, 20});

    // Emit the custom event
    // ecs_emit(ecs, &(ecs_event_desc_t) {
    //     .event = MyEvent,
    //     .ids = &(ecs_type_t){ (ecs_id_t[]){ ecs_id(Position) }, 1 }, // 1 id
    //     .entity = e
    // });

    // Create a system for Position, Velocity.
    ECS_SYSTEM(ecs, Move, EcsOnUpdate, Position);
    ecs_entity_t e1 = ecs_new_entity(ecs, "e1");
    ecs_set(ecs, e1, Position, {10, 20});

    ecs_progress(ecs, 0.0f);

    ecs_fini(ecs);


    // Output
    //    - MyEvent: Position: e
}
