/**
 * This file is generated by xresloader 2.7.3, please don't edit it.
 * You can find more information about this xresloader on https://xresloader.atframe.work/ .
 * If there is any problem, please find or report issues on https://github.com/xresloader/xresloader/issues .
 */
#pragma once

#include "CoreMinimal.h"
#include "UObject/ConstructorHelpers.h"
#include "Engine/DataTable.h"
#include "EventRuleItem.generated.h"


USTRUCT(BlueprintType)
struct FEventRuleItem : public FTableRowBase
{
    GENERATED_USTRUCT_BODY()

    // Start of fields
    /** Field Type: INT, Name: RuleId **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 RuleId;

    /** Field Type: INT, Name: RuleParam **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 RuleParam;

    /** Field Type: int32, Name: Nested **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 Nested;

    /** Field Type: STRING, Name: NestedNote **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    FString NestedNote;

    /** Field Type: INT, Name: NestedEnumType **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 NestedEnumType;

};